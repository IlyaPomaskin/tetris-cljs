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
       "ArrowLeft" (game/move-piece [-1 0] state)
       "ArrowRight" (game/move-piece [1 0] state)
       "ArrowUp" (game/rotate-piece :clockwise state)
       "ArrowDown" (game/rotate-piece :counterclockwise state)
       "Space" (game/hard-drop state)
       state))))


(defn game-loop [] (swap! game-state game/fall))


(defn clear-timer! []
  (swap! game-timer js/clearInterval))


(defn create-timer! [timeout]
  (reset! game-timer (js/setInterval game-loop timeout)))


(defn check-game-over [_ _ _ state]
  (when (game/game-over? state)
    (do
      (clear-timer!)
      (js/window.removeEventListener "keydown" handle-key-press))))


(defn update-speed [_ _ prev-state state]
  (when (not= (game/get-speed prev-state)
              (game/get-speed state))
    (do
      (clear-timer!)
      (create-timer! (game/get-speed state)))))


(defn init []
  (create-timer! (game/get-speed @game-state))
  (js/window.addEventListener "keydown" handle-key-press)
  (add-watch game-state :render ui/render-game)
  (add-watch game-state :state ui/render-state)
  (add-watch game-state :game-over check-game-over)
  (add-watch game-state :speed update-speed))
