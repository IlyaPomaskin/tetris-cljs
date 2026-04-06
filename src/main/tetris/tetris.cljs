(ns tetris.tetris
  (:require [tetris.game :as game]
            [tetris.ui :as ui]))

(enable-console-print!)

(defonce game-state (atom nil))
(defonce prev-timestamp (atom 0))
(defonce bounce-start (atom 0))
(defonce was-falling (atom true))

(defn handle-key-press [event]
  (when (not (game/game-over? @game-state))
    (when (= (.-code event) "Space")
      (reset! bounce-start (js/performance.now)))
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
  (ui/render-game @game-state))

(defn check-game-over [_ _ _ state]
  (when (game/game-over? state)
    (js/window.removeEventListener "keydown" handle-key-press)))

(defn game-loop! [timestamp]
  (when-not (:pause? @game-state)
    (let [diff (- timestamp @prev-timestamp)
          state @game-state
          speed (game/get-speed state)
          next-tick? (> diff speed)]

      (let [can-fall? (game/can-fall? state)]
        (when (and @was-falling (not can-fall?))
          (reset! bounce-start timestamp))
        (reset! was-falling can-fall?)

        (if (and next-tick?
                 (not (game/game-over? state)))
          (do
            (swap! game-state game/fall)
            (reset! prev-timestamp timestamp)
            (let [bounce-elapsed (- timestamp @bounce-start)
                  bounce-offset (ui/calc-bounce-offset bounce-elapsed)]
              (ui/render-game @game-state {:bounce-offset bounce-offset})))
          (let [progress (/ diff speed)
                y-offset (if can-fall?
                           (* progress ui/cell-size)
                           0)
                bounce-elapsed (- timestamp @bounce-start)
                bounce-offset (ui/calc-bounce-offset bounce-elapsed)]
            (ui/render-game state {:y-offset y-offset
                                   :bounce-offset bounce-offset}))))

      (js/requestAnimationFrame game-loop!))))

(def new-game-button (js/document.querySelector "#new-game"))
(.addEventListener
 new-game-button
 "click"
 (fn []
   (.blur new-game-button)
   (reset! game-state (game/create-game))))

(def toggle-noise-checkbox (js/document.querySelector "#toggle-noise"))
(.addEventListener
 toggle-noise-checkbox
 "change"
 (fn []
   (reset! ui/ui-mode (if (.-checked toggle-noise-checkbox) :noise :classic))))

(doall
 (for [btn (array-seq (js/document.querySelectorAll ".random-fill"))]
   (.addEventListener
    btn
    "click"
    #(let [^js dataset (.-dataset btn)
           lines (int (.-lines dataset))
           density (float (.-density dataset))]
       (reset! game-state (game/create-game lines density))
       (.blur btn)))))

(.addEventListener
 js/window
 "focus"
 (fn []
   (swap! game-state assoc :pause? false)
   (js/requestAnimationFrame game-loop!)))

(.addEventListener js/window "blur" (fn [] (swap! game-state assoc :pause? true)))

(defn init []
  (js/window.addEventListener "keydown" handle-key-press)
  (add-watch game-state :game-over check-game-over)
  (reset! game-state (game/create-game))
  (js/requestAnimationFrame game-loop!))
