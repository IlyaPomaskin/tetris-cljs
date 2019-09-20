(ns tetris.game
  (:require [tetris.tetrominoes :as tetrominoes]))


(defn make-piece [piece x]
  (let [piece-width (-> (count (first piece))
                        (/ 2)
                        (Math/floor))]
    {:cells piece
     :x (- x piece-width)
     :y 0}))


(defn use-next-piece [state]
  (let [{stack :stack
         buffer :buffer} state
        field-width (count (first stack))
        [[piece] next-buffer] (split-at 1 buffer)]
    (-> state
        (assoc :buffer (if (empty? next-buffer)
                         (shuffle tetris.tetrominoes/items)
                         next-buffer))
        (assoc :piece (make-piece piece (/ field-width 2))))))


(defn create-game [field-width field-height]
  {:stack (vec (repeat field-height (vec (repeat field-width 0))))
   :piece (make-piece (rand-nth tetrominoes/items) (/ field-width 2))
   :buffer (shuffle tetris.tetrominoes/items)
   :speed 500
   :state :game})


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


(defn place-piece [state]
  (let [{stack :stack
         piece :piece} state
        piece-symbol (->> (get piece :cells)
                          (flatten)
                          (filter #(not (zero? %1)))
                          (first))
        piece-coords (piece->coords piece)
        next-stack (reduce
                    (fn [stack [y x]] (assoc-in stack [y x] piece-symbol))
                    stack
                    piece-coords)]
    (assoc state :stack next-stack)))


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


(defn remove-filled-lines [state]
  (let [stack (get state :stack)
        field-height (count stack)
        field-width (count (first stack))
        stack-wo-lines (filterv #(not-every? pos? %1) stack)
        removed-lines-count (- field-height (count stack-wo-lines))
        new-empty-lines (vec (repeat removed-lines-count (vec (repeat field-width 0))))
        next-stack (into [] (concat new-empty-lines stack-wo-lines))]
    (assoc state :stack next-stack)))


(defn check-game-over [state]
  (let [{stack :stack
         piece :piece} state]
    (if (has-collisions? stack piece)
      (assoc state :state :game-over)
      state)))


(defn next-cycle [state]
  (-> state
      (place-piece)
      (remove-filled-lines)
      (use-next-piece)
      (check-game-over)))


(defn fall [state]
  (let [stack (get state :stack)
        current-piece (get state :piece)
        lowered-piece (update-in current-piece [:y] inc)]
    (if (or (has-collisions? stack lowered-piece)
            (out-of-bounds? stack lowered-piece))
      (next-cycle state)
      (assoc-in state [:piece] lowered-piece))))


(defn hard-drop [state]
  (-> state
      (update-in [:piece] #(drop-piece (get state :stack) %1))
      (next-cycle)))
