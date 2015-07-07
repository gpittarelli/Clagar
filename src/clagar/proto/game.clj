(ns clagar.proto.game
  (:require [clagar.proto.meta.agar :refer [ws-connect get-token]]
            [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.string :as str]
            [clojure.core.async
             :as async
             :refer [>! >!! <! <!! chan go go-loop close!]]))


(defprotocol AgarClient
  (spectate [this])
  (spawn [this name])
  (set-dir [this x y])
  (split [this])
  (eject [this])
  (close [this]))

(deftype GameConnection [state events-ch cmds-ch control-ch]
  AgarClient
  (spectate [this] (go (>! (.cmds-ch this) {:type :spectate})))
  (spawn [this name] (>!! chan {:type :spectate :name name}))
  (set-dir [this x y] (>!! chan {:type :spectate :x x :y y}))
  (split [this] (>!! chan {:type :split}))
  (eject [this] (>!! chan {:type :eject}))
  (close [this] (>!! control-ch :close)))

(defn- state-updater-transducer [state-ref]
  (fn [xf]
    (fn
      ([] (xf))
      ([result] (xf result))
      ([result m]
       ((case (:type m)
          :blob-updates
          (fn []
            (let [removals (concat (map :victim-id (:eats m))
                                   (map :id (:deletes m)))
                  blob-updates (map #(if (empty? (:name %))
                                       (dissoc % :name)
                                       %)
                                    (:updates m))
                  id-updates (into {} (map (juxt :id #(dissoc % :id))
                                           blob-updates))]
              ;; NOTE: state is updated BEFORE events are emitted
              (dosync
               (alter state-ref update-in [:blobs] #(merge-with merge %
                                                                id-updates))
               (alter state-ref update-in [:blobs] #(apply (partial dissoc %)
                                                           removals)))
              (doseq [eat (:eats m)]
                (xf result eat))
              (doseq [update (:updates m)]
                (xf result update))
              (doseq [delete (:deletes m)]
                (xf result delete))))

          :view-update
          (fn [] )

          :reset
          (fn [] )

          :draw-debug-line (fn [] )
          :owns-blob (fn [] )
          :ffa-leaders (fn [] )
          :team-leaders (fn [] )

          :game-area
          (fn []
            (let [new-world (dissoc m :type)]
              (dosync
               (alter state-ref assoc :world new-world))))))))))

(defn connect [region gamemode]
  (go
    (let [events-ch (chan 99999)
          state (ref {:blobs {}
                      :world {:min-x -7071.067811 :min-y -7071.067811
                              :max-x 7071.067811 :max-y 7071.067811}})
          [url token] (<! (get-token region gamemode))
          [incoming outgoing control] (ws-connect url token)]

      (async/pipeline-blocking
       1 events-ch (state-updater-transducer state) incoming)

      ;; (go-loop []
;;         (let [m (<! incoming)]
;;           ((case (:type m)
;;              :blob-updates
;;              (fn []
;;                (let [removals (concat (map :victim-id (:eats m))
;;                                       (map :id (:deletes m)))
;;                      blob-updates (map #(if (empty? (:name %))
;;                                           (dissoc % :name)
;;                                           %)
;;                                        (:updates m))
;;                      id-updates (into {} (map (juxt :id #(dissoc % :id))
;;                                               blob-updates))]
;;                  (dosync
;;                   (alter state update-in [:blobs] #(merge-with merge %
;;                                                                id-updates))
;;                   (alter state update-in [:blobs] #(apply (partial dissoc %)
;;                                                           removals)))))

;;              :view-update
;;              (fn [] )

;;              :reset
;;              (fn [] )

;;              :draw-debug-line (fn [] )
;;              :owns-blob (fn [] )
;;              :ffa-leaders (fn [] )
;;              :team-leaders (fn [] )

;;              :game-area
;;              (fn []
;;                (let [new-world (dissoc m :type)]
;;                  (dosync
;;                   (alter state assoc :world new-world)))))))
;;         (recur))

      (GameConnection. state events-ch outgoing control))))
