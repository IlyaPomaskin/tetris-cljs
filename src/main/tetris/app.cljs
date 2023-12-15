(ns tetris.app
  (:require [tetris.game :as game]
            [tetris.ui :as ui]))

(enable-console-print!)

(def field-width 10)
; TODO hide top 2 rows
(def field-height 22)

(defonce game-state (atom nil))
(defonce prev-timestamp (atom 0))
(defonce is-game-over (atom false))

(defn handle-key-press [event]
  (swap!
   game-state
   (fn [state]
     (case (.-code event)
       "ArrowLeft" (game/move-piece [-1 0] state)
       "ArrowRight" (game/move-piece [1 0] state)
       "ArrowUp" (game/rotate-piece :clockwise state)
       "KeyX" (game/rotate-piece :clockwise state)
       "KeyZ" (game/rotate-piece :counterclockwise state)
       "ArrowDown" (game/soft-drop state)
       "Space" (game/hard-drop state)
       state))))

(defn check-game-over [_ _ _ state]
  (when (game/game-over? state)
    (reset! is-game-over true)
    (js/window.removeEventListener "keydown" handle-key-press)))

(defn game-loop! [timestamp]
  (let [diff (- timestamp @prev-timestamp)
        next-tick? (> diff (game/get-speed @game-state))]

    (when next-tick?
      (swap! game-state game/fall)
      (reset! prev-timestamp timestamp))

    (when (not @is-game-over)
      (js/requestAnimationFrame game-loop!))))

(defn init []
  (js/window.addEventListener "keydown" handle-key-press)
  (add-watch game-state :render ui/render-game)
  (add-watch game-state :state ui/render-state)
  (add-watch game-state :game-over check-game-over)
  (reset! game-state (game/create-game field-width field-height))
  (js/requestAnimationFrame game-loop!))
