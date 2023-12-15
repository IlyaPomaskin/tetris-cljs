(ns tetris.ui
  (:require [clojure.string :as string]
            [tetris.game :as game]
            [tetris.utils :as utils]))

(def stack-container (js/document.querySelector "#stack"))
(def next-pieces-container (js/document.querySelector "#next-pieces"))
(def game-state-container (js/document.querySelector "#game-state"))
(def canvas (js/document.querySelector "#canvas"))
(def ctx (.getContext canvas "2d"))

(defn cell->class [cell]
  (case cell
    :l "cell-filled cell-l"
    :j "cell-filled cell-j"
    :s "cell-filled cell-s"
    :t "cell-filled cell-t"
    :z "cell-filled cell-z"
    :o "cell-filled cell-o"
    :i "cell-filled cell-i"
    :g "cell-filled cell-ghost"
    " "))

(defn render-stack-cell [cell]
  (string/join
   ["<div class='cell " (cell->class cell) "'>"
    (when cell
      "<div class='cell__reflex'></div>")
    "</div>"]))

(defn render-stack [stack]
  (string/join
   (vec (for [[stack-y line] (map-indexed vector stack)]
          (string/join
           (concat
            "<div class='row'>"
            (vec (for [[stack-x] (map-indexed vector line)]
                   (render-stack-cell (get-in stack [stack-y stack-x]))))
            "</div>"))))))

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

(def cell-size 20)

(defn set-color! [color]
  (set! (.-fillStyle ctx) color))

(defn draw-rect! [x y w h]
  (.fillRect ctx x y w h))

(defn c-render-cell [x y piece]
  (set-color! (cell->color piece))
  (draw-rect! (* y cell-size) (* x cell-size) cell-size cell-size))

(defn render-stack-canvas [stack]
  (set-color! "black")
  (draw-rect! 0 0 (* (count (nth stack 0)) cell-size) (* (count stack) cell-size))
  (utils/iterate-stack stack c-render-cell))

(defn render-next-pieces [buffer]
  (let [stack (vec (repeat 22 (vec (repeat 6 nil))))

        buffer-on-bg
        (reduce
         (fn [state [index piece]]
           (let [next-piece (game/make-piece piece 1 (inc (* index 4)))]
             (game/place-piece
              (assoc state :piece next-piece))))
         {:stack stack}
         (map-indexed (fn [index item] [index item]) (take 5 buffer)))]
    (render-stack (:stack buffer-on-bg))))

(defn render-game [_ _ _ state]
  (let [next-state (game/place-piece (game/place-ghost-piece state))]
    ; (set! (.-innerHTML stack-container) (render-stack (:stack next-state)))
    (render-stack-canvas (:stack next-state))
    (set! (.-innerHTML next-pieces-container) (render-next-pieces (:buffer next-state)))))

(defn render-stats [state]
  (let [{score :score
         lines :lines} state]
    (string/join
     "<br/>"
     [(string/join ["level: " (game/get-level lines)])
      (string/join ["lines: " lines])
      (string/join ["score: " score])])))

(defn render-state [_ _ _ state]
  (set!
   (.-innerHTML game-state-container)
   (string/join
    "<br/>"
    [(when (= (get state :state) :game-over)
       "Game over")
     (render-stats state)])))