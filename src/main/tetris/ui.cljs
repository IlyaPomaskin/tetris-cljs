(ns tetris.ui
  (:require [clojure.string :as string]
            [tetris.game :as game]))

(def stack-container (js/document.querySelector "#stack"))
(def next-pieces-container (js/document.querySelector "#next-pieces"))
(def game-state-container (js/document.querySelector "#game-state"))

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
    (set! (.-innerHTML stack-container) (render-stack (:stack next-state)))
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