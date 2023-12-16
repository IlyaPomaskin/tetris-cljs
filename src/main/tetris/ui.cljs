(ns tetris.ui
  (:require [clojure.string :as string]
            [tetris.game :as game]
            [tetris.utils :as utils]))

(def game-state-container (js/document.querySelector "#game-state"))
(def canvas (js/document.querySelector "#canvas"))
(def ctx (.getContext canvas "2d"))

(def cell-size 20)

(def canvas-width (.-width canvas))
(def canvas-height (.-height canvas))

(def field-width (* 10 cell-size))
(def field-height (* 22 cell-size))

(defn cell->color [cell]
  (case cell
    :l "#f0a000"
    :j "#0000f0"
    :s "#00f000"
    :t "#a000f0"
    :z "#f00000"
    :o "#f0f000"
    :i "#0793f7"
    :g "rgba(255, 255, 255, 0.3)"
    "black"))

(defn draw-border! [x y w h color]
  (set! (.-strokeStyle ctx) color)
  (.strokeRect ctx x y w h))

(defn draw-inner-border! [x y w h color]
  (draw-border! (inc x) (inc y) (dec w) (dec h) color))

(defn draw-rect! [x y w h color]
  (set! (.-fillStyle ctx) color)
  (.fillRect ctx x y w h))

(defn c-render-cell
  ([x y piece]
   (c-render-cell x y piece cell-size))

  ([x y piece size]
   (when piece
     (draw-rect! (* x size) (* y size) size size (cell->color piece))
     (draw-border! (* x size) (* y size) size size "black"))))

(defn render-stack [stack]
  (utils/iterate-stack
   stack
   (fn [x y]
     (draw-border!
      (* x cell-size) (* y cell-size)
      cell-size cell-size
      "white")))
  (draw-rect! 0 0 canvas-width canvas-height "rgba(0, 0, 0, 0.8)")
  (utils/iterate-stack stack c-render-cell)
  (draw-inner-border! 0 (* 2 cell-size) field-width (- field-height (* 2 cell-size)) "green"))

(defn render-next-pieces [buffer]
  (let [next-piece-cell-size 10]
    (dorun
     (map-indexed
      (fn [item-index item]
        (dorun
         (map
          (fn [[x y]]
            (c-render-cell
             (+ x 3 game/field-width next-piece-cell-size)
             (+ y 2 (* 5 item-index))
             (:name item) next-piece-cell-size))

          (game/piece->coords (game/make-piece item 0)))))
      (take 5 buffer)))

    (draw-inner-border!
     (* cell-size game/field-width)
     0
     (* cell-size 4)
     (* cell-size 14)
     "white")))

(defn render-text! [x y text size]
  (set! (.-strokeStyle ctx) "white")
  (set! (.-fillStyle ctx) "white")

  (set! (.-font ctx) (string/join ["bold " size "px sans-serif"]))
  (.fillText ctx text x y))

(defn render-stats [state]
  (let [{score :score
         lines :lines} state]
    (render-text! 210 300 (string/join ["level: " (game/get-level lines)]) 10)
    (render-text! 210 314 (string/join ["lines: " lines]) 10)
    (render-text! 210 328 (string/join ["score: " score]) 10)
    (when (= (get state :state) :game-over)
      (render-text! 208 386 "Game over" 12))))

(defn render-game [_ _ _ state]
  (let [next-state (game/place-piece (game/place-ghost-piece state))]
    (.clearRect ctx 0 0 canvas-width canvas-height)
    (render-stack (:stack next-state))
    (draw-inner-border! 0 0 canvas-width canvas-height "blue")
    (render-next-pieces (:buffer next-state))
    (render-stats state)))
