(ns tetris.ui
  (:require [clojure.string :as string]
            [tetris.game :as game]))


(def stack-container (js/document.querySelector "#stack"))
(def game-state-container (js/document.querySelector "#game-state"))


(defn render-stack-cell [cell y x]
  (string/join
   ["<div class='cell " (if (= cell 0) " " (string/join ["cell-" cell])) "'>"
    "</div>"]))


(defn render-stack [stack]
  (string/join
   "<br/>"
   (vec (for [[stack-y line] (map-indexed vector stack)]
          (string/join
           (vec (for [[stack-x cell] (map-indexed vector line)]
                  (render-stack-cell (get-in stack [stack-y stack-x]) stack-y stack-x))))))))


(defn render-game [_ _ _ state]
  (let [stack-with-piece (get (game/place-piece state) :stack)]
    (set! (.-innerHTML stack-container) (render-stack stack-with-piece))))


(defn render-state [_ _ _ state]
  (let [{state :state
         speed :speed} state]
    (set!
     (.-innerHTML game-state-container)
     (if (= state :game)
       (string/join " " ["speed" speed])
       "game over"))))