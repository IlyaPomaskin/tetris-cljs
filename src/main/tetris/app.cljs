(ns tetris.app
  (:require [clojure.string :as string]
            [tetris.tetrominos :as tetrominos]))

(enable-console-print!)

(def field-width 10)
(def field-height 10)

(defn create-new-piece []
  {:cells (rand-nth tetrominos/items)
   :x (/ field-width 2)
   :y 0})

(defonce game-state
  (atom
   {:stack (vec (repeat field-height (vec (repeat field-width 0))))
    :piece (create-new-piece)
    :state :game}))

(defn piece->coords [piece]
  (let [{cells :cells
         piece-x :x
         piece-y :y} piece]
    (for [[cell-y line] (map-indexed vector cells)
          [cell-x cell] (map-indexed vector line)
          :let [stack-y (+ piece-y cell-y)
                stack-x (+ piece-x cell-x)]
          :when (not (zero? (get-in cells [cell-y cell-x])))]
      [stack-y stack-x])))

(defn game-over? [state]
  (= (get state :state) :game-over))

(defn has-collisions? [stack piece]
  (->> (piece->coords piece)
       (map #(get-in stack %1))
       (filter pos?)
       (first)
       (some?)))

(defn out-of-bounds? [stack piece]
  (let [{cells :cells
         piece-x :x
         piece-y :y} piece
        piece-height (count cells)
        stack-height (count stack)
        out-of-y-bound? (> (+ piece-y piece-height)
                           stack-height)
        piece-width (count (peek cells))
        stack-width (count (peek stack))
        out-of-x-bound? (or (> (+ piece-x piece-width)
                               stack-width)
                            (< (+ piece-x piece-width)
                               0))]
    (or out-of-y-bound? out-of-x-bound?)))

(defn place-piece [stack piece]
  (let [piece-symbol (->> (get piece :cells)
                          (flatten)
                          (filter #(not (zero? %1)))
                          (first))
        piece-coords (piece->coords piece)]
    (reduce
     (fn [stack [y x]] (assoc-in stack [y x] piece-symbol))
     stack
     piece-coords)))

(defn rotate-clockwise [cells]
  (vec (mapv #(vec (reverse %1)) (apply mapv vector cells))))

(defn rotate-anticlockwise [cells]
  (vec (apply mapv vector (mapv #(vec (reverse %1)) cells))))

(defn rotate-piece [direction state]
  (let [piece (get state :piece)
        stack (get state :stack)
        cells (get piece :cells)
        next-cells (case direction
                     :up (rotate-anticlockwise cells)
                     :down (rotate-clockwise cells)
                     cells)
        next-piece (assoc piece :cells next-cells)]
    (if (or (has-collisions? stack next-piece)
            (out-of-bounds? stack next-piece))
      state
      (assoc-in state [:piece] next-piece))))

(defn move-piece [offset state]
  (let [piece (get state :piece)
        stack (get state :stack)
        piece-width (count (peek (get piece :cells)))
        offseted-x (+ (get piece :x) offset)
        stack-width (count (peek stack))
        next-x (max 0 (min (- stack-width piece-width) offseted-x))
        next-piece (assoc piece :x next-x)]
    (if (or (has-collisions? stack next-piece)
            (out-of-bounds? stack next-piece))
      state
      (assoc-in state [:piece] next-piece))))

(defn drop-piece [stack piece]
  (let [last-y (->> (range (get piece :y) (count stack))
                    (filter #(or (has-collisions? stack (assoc piece :y (+ 1 %1)))
                                 (out-of-bounds? stack (assoc piece :y (+ 1 %1)))))
                    (first))]
    (assoc piece :y last-y)))

(defn remove-filled-lines [stack]
  (let [stack-wo-lines (filterv #(not-every? pos? %1) stack)
        removed-lines-count (- field-height (count stack-wo-lines))]
    (into [] (concat (vec (repeat removed-lines-count (vec (repeat field-width 0))))
                     stack-wo-lines))))

(defn check-game-over [state]
  (let [stack (get state :stack)
        piece (get state :piece)]
    (if (has-collisions? stack piece)
      (assoc state :state :game-over)
      state)))

(defn next-cycle [state]
  (-> state
      (update-in [:stack] place-piece (get state :piece))
      (update-in [:stack] remove-filled-lines)
      (update-in [:piece] create-new-piece)
      (check-game-over)))

(defn fall [state]
  (let [stack (get state :stack)
        current-piece (get state :piece)
        lowered-piece (update-in current-piece [:y] inc)]
    (if (or (has-collisions? stack lowered-piece)
            (out-of-bounds? stack lowered-piece))
      (next-cycle state)
      (assoc-in state [:piece] lowered-piece))))

(defn handle-key-press [event]
  (let [key (.-code event)
        state @game-state
        next-state (case key
                     "ArrowLeft" (move-piece -1 state)
                     "ArrowRight" (move-piece 1 state)
                     "ArrowUp" (rotate-piece :up state)
                     "ArrowDown" (rotate-piece :down state)
                     "Space" (-> state
                                 (update-in [:piece] #(drop-piece (get state :stack) %1))
                                 (next-cycle))
                     state)]
    (if (game-over? state)
      state
      (reset! game-state next-state))))

(defn game-loop []
  (swap! game-state #(if (game-over? %1) %1 (fall %1))))

(defn init []
  (js/window.addEventListener "keydown" handle-key-press)
  (js/setInterval game-loop 1000)
  (js/console.log "init"))

(def stack-container (js/document.querySelector "#stack"))
(def game-state-container (js/document.querySelector "#game-state"))
(defn render-stack-cell [cell y x]
  (string/join
   ["<div class='btn " (if (= cell 0) " " (string/join ["btn-" cell])) "'>"
    "</div>"]))
(defn render-stack [stack]
  (string/join
   "\n"
   (vec (for [[stack-y line] (map-indexed vector stack)]
          (string/join
           (vec (for [[stack-x cell] (map-indexed vector line)]
                  (render-stack-cell (get-in stack [stack-y stack-x]) stack-y stack-x))))))))
(defn render-game [_ _ _ state]
  (set! (.-innerHTML stack-container) (render-stack (place-piece (get state :stack) (get state :piece)))))
(add-watch game-state :render render-game)
(defn render-state [_ _ _ state]
  (set! (.-innerHTML game-state-container) (get state :state)))
(add-watch game-state :state render-state)