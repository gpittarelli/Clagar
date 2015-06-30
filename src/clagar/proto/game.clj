(ns clagar.proto.game)

(defn connect [{& {:keys [region gamemode url]
                   :or {region ""
                        gamemode "FFA"
                        url ""}}}]

  )

(deftype GameConnection [state chan]
  ())
