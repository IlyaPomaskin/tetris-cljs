(ns tetris.app
  (:require [tetris.game :as game]
            [tetris.ui :as ui]))


(enable-console-print!)


(def field-width 10)
(def field-height 16)


(defonce game-state (atom (game/create-game field-width field-height)))
(defonce game-timer (atom nil))


(defn handle-key-press [event]
  (swap!
   game-state
   (fn [state]
     (case (.-code event)
       "ArrowLeft" (game/move-piece -1 state)
       "ArrowRight" (game/move-piece 1 state)
       "ArrowUp" (game/rotate-piece :up state)
       "ArrowDown" (game/rotate-piece :down state)
       "Space" (game/hard-drop state)
       state))))


(defn game-loop [] (swap! game-state game/fall))


(defn check-game-over [_ _ _ state]
  (when (game/game-over? state)
    (do
      (js/clearInterval @game-timer)
      (js/window.removeEventListener "keydown" handle-key-press))))


(defn update-speed [_ _ prev-state state]
  (when (not= (get prev-state :speed)
              (get state :speed))
    (do
      (js/clearInterval @game-timer)
      (reset! game-timer (js/setInterval game-loop (get @game-state :speed))))))


(defn init []
  (reset! game-timer (js/setInterval game-loop (get @game-state :speed)))
  (js/window.addEventListener "keydown" handle-key-press)
  (add-watch game-state :render ui/render-game)
  (add-watch game-state :state ui/render-state)
  (add-watch game-state :game-over check-game-over)
  (add-watch game-state :speed update-speed))
