(ns tetris.ui
  (:require
   [clojure.string :as string]
   [tetris.game :as game]
   [tetris.utils :as utils]))

(def canvas (js/document.querySelector "#canvas"))
(def ctx (.getContext canvas "2d"))

(def cell-size 20)
(def next-piece-cell-size 10)

(def canvas-width (.-width canvas))
(def canvas-height (.-height canvas))

(def field-width (* 10 cell-size))
(def field-height (* 22 cell-size))

(def black "black")

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

(defn draw-cell!
  ([x y piece]
   (draw-cell! x y piece cell-size))

  ([x y piece size]
   (when piece
     (let [x' (* x size)
           y' (* y size)]
       (draw-rect! x' y' size size (cell->color piece))
       (draw-rect! (+ 1 x') (+ 2 y') (- size 0) (- size 0) "rgba(0, 0, 0, 0.2)")
       (draw-border! x' y' size size "rgba(0, 0, 0, 0.2)")))))

(defn render-stack [stack]
  (draw-rect! 0 0 canvas-width canvas-height black)
  (utils/iterate-stack
   stack
   (fn [x y]
     (draw-border! (* x cell-size) (* y cell-size) cell-size cell-size "rgba(255, 255, 255, 0.05)")))
  (utils/iterate-stack stack (fn [x y piece] (draw-cell! x (- y 2) piece)))
  (draw-inner-border! 0 0 field-width (- field-height (* 2 cell-size)) "white"))

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
        offset-y 200]
    (draw-text! 210 offset-y 10 (string/join ["level: " (game/get-level lines)]))
    (draw-text! 210 (+ offset-y 14) 10 (string/join ["lines: " lines]))
    (draw-text! 210 (+ offset-y 28) 10 (string/join ["score: " score]))
    (when (= (get state :state) :game-over)
      (draw-text! 208 (+ offset-y 120) 12 "Game over"))))

(defn render-game [_ _ _ state]
  (let [next-state (game/place-piece (game/place-ghost-piece state))]
    (draw-rect! 0 0 canvas-width canvas-height black)
    (render-stack (:stack next-state))
    (render-next-pieces (:buffer next-state))
    (render-stats state)
    (draw-inner-border! 0 0 canvas-width canvas-height "white")))
