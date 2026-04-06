(ns tetris.tetris
  (:require [tetris.game :as game]
            [tetris.ui :as ui]))

(enable-console-print!)

(defonce game-state (atom nil))
(defonce prev-timestamp (atom 0))
(defonce bounce-start (atom 0))
(defonce was-falling (atom true))
(defonce clear-start (atom 0))
(defonce clearing-rows (atom []))
(defonce soft-dropping (atom false))
(defonce x-anim-start (atom 0))
(defonce x-anim-direction (atom 0))
(defonce pre-clear-state (atom nil))

(def x-anim-duration 80)

(defn calc-x-offset [timestamp]
  (let [elapsed (- timestamp @x-anim-start)]
    (if (< elapsed x-anim-duration)
      (* @x-anim-direction ui/cell-size (- 1.0 (/ elapsed x-anim-duration)))
      0)))

(def soft-drop-speed 50)

(defn adjust-timestamp-for-speed-change! [old-speed new-speed]
  (let [now (js/performance.now)
        diff (- now @prev-timestamp)
        progress (min 1.0 (/ diff old-speed))
        new-diff (* progress new-speed)]
    (reset! prev-timestamp (- now new-diff))))

(defn lock-piece! [timestamp]
  (let [placed (game/place-only @game-state)
        filled (game/get-filled-rows (:stack placed))
        noise? (= @ui/ui-mode :noise)]
    (if (and noise? (seq filled))
      (do
        (reset! clearing-rows filled)
        (reset! clear-start timestamp)
        (reset! pre-clear-state placed)
        (reset! game-state placed))
      (do
        (reset! game-state (game/continue-after-clear placed))))))

(defn handle-key-press [event]
  (when (contains? #{"ArrowUp" "ArrowDown" "ArrowLeft" "ArrowRight" "Space"} (.-code event))
    (.preventDefault event))
  (when (not (game/game-over? @game-state))
    (if (= (.-code event) "Space")
      (let [now (js/performance.now)]
        (swap! game-state game/hard-drop)
        (reset! bounce-start now)
        (lock-piece! now))
      (let [old-x (get-in @game-state [:piece :x])]
        (swap!
         game-state
         (fn [state]
           (case (.-code event)
             "ArrowLeft" (game/move-piece! [-1 0] state)
             "ArrowRight" (game/move-piece! [1 0] state)
             "ArrowUp" (game/rotate-piece :clockwise state)
             "KeyX" (game/rotate-piece :clockwise state)
             "KeyZ" (game/rotate-piece :counterclockwise state)
             "ArrowDown" (do
                          (when (not @soft-dropping)
                            (adjust-timestamp-for-speed-change!
                             (game/get-speed state) soft-drop-speed)
                            (reset! soft-dropping true))
                          state)
             state)))
        (let [new-x (get-in @game-state [:piece :x])]
          (when (not= old-x new-x)
            (reset! x-anim-direction (- old-x new-x))
            (reset! x-anim-start (js/performance.now))))))))

(defn handle-key-up [event]
  (when (and (= (.-code event) "ArrowDown") @soft-dropping)
    (adjust-timestamp-for-speed-change!
     soft-drop-speed (game/get-speed @game-state))
    (reset! soft-dropping false)))

(defn check-game-over [_ _ _ state]
  (when (game/game-over? state)
    (js/window.removeEventListener "keydown" handle-key-press)))

(defn clearing? [timestamp]
  (and (seq @clearing-rows)
       (< (- timestamp @clear-start) ui/line-clear-duration)))

(defn finish-clear! [timestamp]
  (when (seq @clearing-rows)
    (reset! bounce-start timestamp)
    (reset! game-state (game/continue-after-clear @pre-clear-state))
    (reset! clearing-rows [])
    (reset! pre-clear-state nil)))

(defn game-loop! [timestamp]
  (when-not (:pause? @game-state)
    (if (clearing? timestamp)
      (let [elapsed (- timestamp @clear-start)
            progress (/ elapsed ui/line-clear-duration)]
        (ui/render-game @game-state {:clearing-rows @clearing-rows
                                     :clear-progress progress}))
      (do
        (when (seq @clearing-rows)
          (finish-clear! timestamp))
        (let [diff (- timestamp @prev-timestamp)
              state @game-state
              base-speed (game/get-speed state)
              speed (if @soft-dropping (min soft-drop-speed base-speed) base-speed)
              next-tick? (> diff speed)
              can-fall? (game/can-fall? state)]

          (when (and @was-falling (not can-fall?))
            (reset! bounce-start timestamp))
          (reset! was-falling can-fall?)

          (if (and next-tick?
                   (not (game/game-over? state)))
            (do
              (if (not can-fall?)
                (lock-piece! timestamp)
                (do
                  (swap! game-state game/fall)
                  (when (not (game/can-fall? @game-state))
                    (reset! bounce-start timestamp)
                    (lock-piece! timestamp))))
              (reset! prev-timestamp timestamp)
              (let [bounce-elapsed (- timestamp @bounce-start)
                    bounce-offset (ui/calc-bounce-offset bounce-elapsed)
                    x-offset (calc-x-offset timestamp)]
                (ui/render-game @game-state {:bounce-offset bounce-offset
                                             :x-offset x-offset})))
            (let [progress (min 1.0 (/ diff speed))
                  y-offset (if can-fall?
                             (* progress ui/cell-size)
                             0)
                  bounce-elapsed (- timestamp @bounce-start)
                  bounce-offset (ui/calc-bounce-offset bounce-elapsed)
                  x-offset (calc-x-offset timestamp)]
              (ui/render-game state {:y-offset y-offset
                                     :bounce-offset bounce-offset
                                     :x-offset x-offset}))))))

    (js/requestAnimationFrame game-loop!)))

(def new-game-button (js/document.querySelector "#new-game"))
(.addEventListener
 new-game-button
 "click"
 (fn []
   (.blur new-game-button)
   (js/window.addEventListener "keydown" handle-key-press)
   (js/window.addEventListener "keyup" handle-key-up)
   (reset! soft-dropping false)
   (reset! game-state (game/create-game))))

(def toggle-noise-checkbox (js/document.querySelector "#toggle-noise"))
(set! (.-checked toggle-noise-checkbox) (= @ui/ui-mode :noise))
(.addEventListener
 toggle-noise-checkbox
 "keydown"
 (fn [e] (.preventDefault e)))
(.addEventListener
 toggle-noise-checkbox
 "change"
 (fn []
   (.blur toggle-noise-checkbox)
   (let [noise? (.-checked toggle-noise-checkbox)]
     (reset! ui/ui-mode (if noise? :noise :classic))
     (set! (.-hash js/location) (if noise? "noise" "")))))

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

(.addEventListener js/window "blur" (fn [] (reset! soft-dropping false) (swap! game-state assoc :pause? true)))

(defn init []
  (js/window.addEventListener "keydown" handle-key-press)
  (js/window.addEventListener "keyup" handle-key-up)
  (add-watch game-state :game-over check-game-over)
  (reset! game-state (game/create-game))
  (js/requestAnimationFrame game-loop!))
