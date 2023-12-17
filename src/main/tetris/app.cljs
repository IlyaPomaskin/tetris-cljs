(ns tetris.app
  (:require [tetris.game :as game]
            [tetris.ui :as ui]))

(enable-console-print!)

(defonce game-state (atom nil))
(defonce prev-timestamp (atom 0))

(defn handle-key-press [event]
  (when (not (game/game-over? @game-state))
    (swap!
     game-state
     (fn [state]
       (case (.-code event)
         "ArrowLeft" (game/move-piece! [-1 0] state)
         "ArrowRight" (game/move-piece! [1 0] state)
         "ArrowUp" (game/rotate-piece :clockwise state)
         "KeyX" (game/rotate-piece :clockwise state)
         "KeyZ" (game/rotate-piece :counterclockwise state)
         "ArrowDown" (game/soft-drop state)
         "Space" (game/hard-drop state)
         state))))
  (ui/render-game nil nil nil @game-state))

(defn check-game-over [_ _ _ state]
  (when (game/game-over? state)
    (js/window.removeEventListener "keydown" handle-key-press)))

(defn game-loop! [timestamp]
  (let [diff (- timestamp @prev-timestamp)
        state @game-state
        next-tick? (> diff (game/get-speed state))]

    (ui/render-game nil nil nil state)

    (when (and next-tick?
               (not (game/game-over? state)))
      (swap! game-state game/fall)
      (reset! prev-timestamp timestamp)
      (ui/render-game nil nil nil state))

    (js/requestAnimationFrame game-loop!)))

(def new-game-button (js/document.querySelector "#new-game"))
(.addEventListener
 new-game-button
 "click"
 (fn []
   (.blur new-game-button)
   (reset! game-state (game/create-game))))

(defn init []
  (js/window.addEventListener "keydown" handle-key-press)
  (add-watch game-state :render ui/render-game)
  (add-watch game-state :game-over check-game-over)
  (reset! game-state (game/create-game))
  (js/requestAnimationFrame game-loop!))
