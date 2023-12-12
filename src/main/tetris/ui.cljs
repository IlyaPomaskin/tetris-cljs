(ns tetris.ui
  (:require [clojure.string :as string]
            [tetris.game :as game]))

(def stack-container (js/document.querySelector "#stack"))
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
    " "))

(defn render-stack-cell [cell]
  (string/join
   ["<div class='cell " (cell->class cell) "'>"
    (when cell
      "<div class='cell__reflex'></div>")
    "</div>"]))

(defn render-stack [stack]
  (string/join
   "<br/>"
   (vec (for [[stack-y line] (map-indexed vector stack)]
          (string/join
           (vec (for [[stack-x] (map-indexed vector line)]
                  (render-stack-cell (get-in stack [stack-y stack-x])))))))))

(defn render-game [_ _ _ state]
  (let [stack-with-piece (get (game/place-piece state) :stack)]
    (set! (.-innerHTML stack-container) (render-stack stack-with-piece))))

(defn render-stats [state]
  (let [{score :score
         lines :lines} state]
    (string/join
     "<br/>"
     [(string/join ["level: " (game/get-level lines)])
      (string/join ["lines: " lines])
      (string/join ["scoreasdf: " score])])))

(defn render-state [_ _ _ state]
  (set!
   (.-innerHTML game-state-container)
   (string/join
    "<br/>"
    [(when (= (get state :state) :game-over)
       "Game over")
     (render-stats state)])))