(ns tetris.app
  (:require [tetris.game :as game]
            [tetris.ui :as ui]))


(enable-console-print!)


(def field-width 10)
(def field-height 16)


(defonce game-state
  (atom (game/create-game field-width field-height)))


(add-watch game-state :render ui/render-game)
(add-watch game-state :state ui/render-state)


(defn handle-key-press [event]
  (let [key (.-code event)
        state @game-state
        next-state (case key
                     "ArrowLeft" (game/move-piece -1 state)
                     "ArrowRight" (game/move-piece 1 state)
                     "ArrowUp" (game/rotate-piece :up state)
                     "ArrowDown" (game/rotate-piece :down state)
                     "Space" (game/hard-drop state)
                     state)]
    (if (game/game-over? state)
      state
      (reset! game-state next-state))))


(defn game-loop []
  (swap! game-state #(if (game/game-over? %1) %1 (game/fall %1))))


(defn init []
  (js/window.addEventListener "keydown" handle-key-press)
  (js/setInterval game-loop 1000)
  (js/console.log "init"))
