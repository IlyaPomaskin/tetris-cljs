(ns tetris.game
  (:require [tetris.tetrominoes :as tetrominoes]))


(def lines-per-level 20)
(def base-timer 750)
(def timer-ms-per-level 50)


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
                         (shuffle tetrominoes/items)
                         next-buffer))
        (assoc :piece (make-piece piece (/ field-width 2))))))


(defn create-game [field-width field-height]
  (use-next-piece
   {:stack (vec (repeat field-height (vec (repeat field-width 0))))
    :piece nil
    :buffer (shuffle tetrominoes/items)
    :score 0
    :lines 0
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


(defn rotate-counterclockwise [cells]
  (vec (apply mapv vector (mapv #(vec (reverse %1)) cells))))


(defn rotate-piece [direction state]
  (let [{stack :stack
         piece :piece} state
        cells (get piece :cells)
        next-cells (case direction
                     :clockwise (rotate-clockwise cells)
                     :counterclockwise (rotate-counterclockwise cells)
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


(defn drop-piece [state]
  (let [{stack :stack
         piece :piece} state
        last-y (->> (range (get piece :y) (count stack))
                    (filter #(or (has-collisions? stack (assoc piece :y (+ 1 %1)))
                                 (out-of-bounds? stack (assoc piece :y (+ 1 %1)))))
                    (first))]
    (assoc-in state [:piece :y] last-y)))


(defn get-level [lines]
  (Math/ceil (/ (+ 1 lines) lines-per-level)))


(defn get-speed [state]
  (let [lines (get state :lines)
        level (get-level lines)]
    (- base-timer (* level timer-ms-per-level))))


(defn get-score [lines-count level]
  (* level
     (case lines-count
       1 40
       2 100
       3 300
       4 1200
       0)))


(defn remove-filled-lines [state]
  (let [{stack :stack
         lines :lines} state
        field-height (count stack)
        field-width (count (first stack))
        stack-wo-lines (filterv #(not-every? pos? %1) stack)
        removed-lines-count (- field-height (count stack-wo-lines))
        new-empty-lines (vec (repeat removed-lines-count (vec (repeat field-width 0))))
        next-stack (into [] (concat new-empty-lines stack-wo-lines))
        next-lines (+ lines removed-lines-count)
        score (get-score removed-lines-count (get-level next-lines))]
    (-> state
        (assoc :stack next-stack)
        (assoc :lines next-lines)
        (update-in [:score] + score))))


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
      (drop-piece)
      (next-cycle)))
