(ns tetris.ui
  (:require
   [clojure.string :as string]
   [tetris.game :as game]
   [tetris.utils :as utils]))

(def canvas (js/document.querySelector "#canvas"))
(def ctx (.getContext canvas "2d"))
(set! (.-imageSmoothingEnabled ctx) false)

(def cell-size 20)
(def next-piece-cell-size 10)

(def canvas-width (.-width canvas))
(def canvas-height (.-height canvas))

(def field-width (* 10 cell-size))
(def field-height (* 22 cell-size))

(def black "black")

(defonce ui-mode (atom (if (= (.-hash js/location) "#noise") :noise :classic)))

(defn cell->color [cell]
  (case cell
    ; orange
    :l "#ff5714"
    ; blue
    :j "#008bf8"
    ; green
    :s "#04e762"
    ; purple
    :t "#9016d2"
    ; red
    :z "#dc0073"
    ; yellow
    :o "#faff00"
    ; cyan
    :i "#1be7ff"
    :g "rgba(255, 255, 255, 0.2)"
    black))

(defn draw-text! [x y size text]
  (set! (.-strokeStyle ctx) "white")
  (set! (.-fillStyle ctx) "white")
  (set! (.-font ctx) (string/join ["bold " size "px sans-serif"]))
  (.fillText ctx text x y))

(defn draw-border!
  ([x y w h color]
   (draw-border! x y w h 1 color))

  ([x y w h width color]
   (set! (.-lineWidth ctx) width)
   (set! (.-strokeStyle ctx) color)
   (.strokeRect ctx x y w h)))

(defn draw-inner-border! [x y w h color]
  (draw-border! (inc x) (inc y) (dec w) (dec h) color))

(defn draw-rect! [x y w h color]
  (set! (.-fillStyle ctx) color)
  (.fillRect ctx x y w h))

(defn generate-noise [w h]
  (let [image-data (.createImageData ctx w h)
        data (.-data image-data)]
    (loop [i 0]
      (when (< i (.-length data))
        (let [v (if (> (js/Math.random) 0.5) 255 0)]
          (aset data i v)
          (aset data (+ i 1) v)
          (aset data (+ i 2) v)
          (aset data (+ i 3) 255))
        (recur (+ i 4))))
    image-data))

(def noise-image (generate-noise field-width (- (* 22 cell-size) (* 2 cell-size))))

(defn image-data->canvas [image-data]
  (let [c (js/document.createElement "canvas")]
    (set! (.-width c) (.-width image-data))
    (set! (.-height c) (.-height image-data))
    (.putImageData (.getContext c "2d") image-data 0 0)
    c))

(def noise-cells (vec (repeatedly 100 #(image-data->canvas (generate-noise cell-size cell-size)))))

(defn noise-index [x y]
  (mod (+ (* x 7) (* y 13)) 100))

(defn noise-cell-at [x y]
  (nth noise-cells (noise-index x y)))

(defn draw-cell!
  ([x y piece]
   (draw-cell! x y piece cell-size))

  ([x y piece size]
   (when piece
     (let [x' (* x size)
           y' (* y size)]
       (if (= @ui-mode :noise)
         (do
           (.drawImage ctx (noise-cell-at x y) x' y')
           #_(draw-border! x' y' size size "rgba(255, 255, 255, 0.3)"))
           
         (do
           (draw-rect! x' y' size size (cell->color piece))
           (draw-rect! (+ 1 x') (+ 2 y') size size "rgba(0, 0, 0, 0.2)")
           (draw-border! x' y' size size "rgba(0, 0, 0, 0.2)")))))))

(def bounce-duration 300)
(def bounce-amplitude 8)
(def line-clear-duration 300)

(defn calc-bounce-offset [elapsed]
  (if (or (not= @ui-mode :noise) (<= elapsed 0) (>= elapsed bounce-duration))
    0
    (let [t (/ elapsed bounce-duration)]
      (* bounce-amplitude (js/Math.sin (* t js/Math.PI))))))

(defn render-stack [stack bounce-offset clearing-rows clear-progress]
  (let [stack-h (- field-height (* 2 cell-size))
        noise? (= @ui-mode :noise)
        clearing-set (set clearing-rows)]
    (draw-rect! 0 0 canvas-width canvas-height black)
    (when noise?
      (.putImageData ctx noise-image 0 0))
    (utils/iterate-stack
     stack
     (fn [x y]
       (draw-border! (* x cell-size) (* y cell-size) cell-size cell-size "rgba(255, 255, 255, 0.05)")))
    (.save ctx)
    (.beginPath ctx)
    (.rect ctx 0 0 field-width stack-h)
    (.clip ctx)
    (utils/iterate-stack
     stack
     (fn [x y cell]
       (when cell
         (let [clearing? (and noise? (contains? clearing-set y))
               x-offset (if clearing? (* (- clear-progress) field-width) 0)
               x' (+ (* x cell-size) x-offset)
               y' (+ (* (- y 2) cell-size) (if clearing? 0 bounce-offset))]
           (if noise?
             (let [idx (if (map? cell) (:noise cell) (noise-index x y))]
               (.drawImage ctx (nth noise-cells idx) x' y')
               (draw-border! x' y' cell-size cell-size "rgba(255, 255, 255, 0.3)"))
             (let [color (if (map? cell) (:type cell) cell)]
               (draw-rect! x' y' cell-size cell-size (cell->color color))
               (draw-rect! (+ 1 x') (+ 2 y') cell-size cell-size "rgba(0, 0, 0, 0.2)")
               (draw-border! x' y' cell-size cell-size "rgba(0, 0, 0, 0.2)")))))))
    (.restore ctx)
    (draw-inner-border! 0 0 field-width stack-h "white")))

(defn render-next-pieces [buffer]
  (->> buffer
       (take 5)
       (map-indexed
        (fn [index item]
          (let [name (:name item)]
            [index
             name
             (game/piece->coords (game/make-piece item 0))])))
       (mapv
        (fn [[index name coords]]
          (mapv
           (fn [[y x]]
             (draw-cell!
              (+ x 2 game/field-width next-piece-cell-size)
              (+ y 2 (* 3 index))
              name next-piece-cell-size))
           coords))))

  (draw-inner-border!
   (* cell-size game/field-width) 0
   (* cell-size 4) (* cell-size 9)
   "white"))

(defn render-stats [state]
  (let [{score :score
         lines :lines} state
        offset-y 200
        game-over? (= (:state state) :game-over)
        pause? (:pause? state)]
    (draw-text! 210 offset-y 10 (string/join ["level: " (game/get-level lines)]))
    (draw-text! 210 (+ offset-y 14) 10 (string/join ["lines: " lines]))
    (draw-text! 210 (+ offset-y 28) 10 (string/join ["score: " score]))
    (when (and pause? (not game-over?))
      (draw-text! 218 (+ offset-y 120) 12 "Pause"))
    (when game-over?
      (draw-text! 208 (+ offset-y 120) 12 "Game over"))))

(defn render-piece [piece y-offset bounce-offset]
  (let [coords (game/piece->coords piece)
        name (get-in piece [:piece :name])
        stack-h (- field-height (* 2 cell-size))
        noise? (= @ui-mode :noise)]
    (.save ctx)
    (.beginPath ctx)
    (.rect ctx 0 0 field-width stack-h)
    (.clip ctx)
    (let [piece-x (:x piece)
          piece-y (:y piece)]
      (doseq [[y x] coords]
        (let [x' (* x cell-size)
              y' (js/Math.round (+ (* (- y 2) cell-size) y-offset bounce-offset))
              local-x (- x piece-x)
              local-y (- y piece-y)]
          (if noise?
             (.drawImage ctx (noise-cell-at local-x local-y) x' y')
            (do
              (draw-rect! x' y' cell-size cell-size (cell->color name))
              (draw-rect! (+ 1 x') (+ 2 y') cell-size cell-size "rgba(0, 0, 0, 0.2)")
              (draw-border! x' y' cell-size cell-size "rgba(0, 0, 0, 0.2)"))))))
    (.restore ctx)))

(defn render-game [state & [{:keys [y-offset bounce-offset clearing-rows clear-progress]}]]
  (let [y-offset (or y-offset 0)
        bounce-offset (or bounce-offset 0)
        clearing-rows (or clearing-rows [])
        clear-progress (or clear-progress 0)]
    (draw-rect! 0 0 canvas-width canvas-height black)
    (render-stack (:stack state) bounce-offset clearing-rows clear-progress)
    (when (:piece state)
      (render-piece (:piece state) y-offset bounce-offset))
    (when (not= @ui-mode :noise)
      (render-next-pieces (:buffer state)))
    (render-stats state)
    (draw-inner-border! 0 0 canvas-width canvas-height "white")))
