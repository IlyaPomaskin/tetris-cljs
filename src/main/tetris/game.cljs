(ns tetris.game
  (:require [tetris.tetrominoes :as tetrominoes]))

(def lines-per-level 20)
(def base-timer 750)
(def timer-ms-per-level 50)

(defn get-rotated-piece [piece]
  (get-in piece [:piece (get piece :rotation)]))

(defn make-piece
  ([piece x]
   (let [piece-width (count (nth (:0 piece) 0))
         piece-half-width (Math/floor (/ 2 piece-width))
         next-x (- x piece-half-width)]
     (make-piece piece next-x 0)))
  ([piece x y]
   {:piece piece
    :rotation :0
    :x x
    :y y}))

(defn use-next-piece [state]
  (let [{stack :stack
         buffer :buffer} state
        field-width (count (first stack))
        [[piece] next-buffer] (split-at 1 buffer)]
    (-> state
        (assoc :buffer (if (<= (count next-buffer) 7)
                         (concat next-buffer (shuffle tetrominoes/items))
                         next-buffer))
        ; Move one cell down if nothing in the way
        (assoc :piece (make-piece piece (/ field-width 2))))))

(defn create-game [field-width field-height]
  (use-next-piece
   {:stack (vec (repeat field-height (vec (repeat field-width nil))))
    :piece nil
    :buffer (concat [(rand-nth tetrominoes/safe-first-items)]
                    (shuffle tetrominoes/items)
                    (shuffle tetrominoes/items))
    :score 0
    :lines 0
    :state :game}))

(defn piece->coords [piece]
  (let [{piece-x :x
         piece-y :y} piece
        cells (get-rotated-piece piece)]
    (for [[cell-y line] (map-indexed vector cells)
          [cell-x] (map-indexed vector line)
          :let [stack-y (+ piece-y cell-y)
                stack-x (+ piece-x cell-x)]
          :when (not (zero? (get-in cells [cell-y cell-x])))]
      [stack-y stack-x])))

(defn game-over? [state]
  (= (get state :state) :game-over))

(defn out-of-bounds? [stack piece]
  (let [stack-height (count stack)
        stack-width (count (peek stack))]
    (->> (piece->coords piece)
         (filter
          (fn [[y x]]
            (or (>= y stack-height)
                (>= x stack-width)
                (< x 0))))
         (first)
         (some?))))

(defn has-collisions? [stack piece]
  (->> (piece->coords piece)
       (map #(get-in stack %1))
       (filter some?)
       (first)
       (some?)))

(defn can-place? [stack piece]
  (and (not (has-collisions? stack piece))
       (not (out-of-bounds? stack piece))))

(defn place-piece [state]
  (let [{stack :stack
         piece :piece} state
        piece-symbol (get-in piece [:piece :name])
        next-stack (reduce
                    (fn [stack [y x]] (assoc-in stack [y x] piece-symbol))
                    stack
                    (piece->coords piece))]
    (assoc state :stack next-stack)))

(defn rotate-clockwise [cells]
  (vec (mapv #(vec (reverse %1)) (apply mapv vector cells))))

(defn rotate-counterclockwise [cells]
  (vec (apply mapv vector (mapv #(vec (reverse %1)) cells))))

(def rotation-clockwise
  {:0 :r
   :r :2
   :2 :l
   :l :0})

(def rotation-counterclockwise
  {:l :2
   :2 :r
   :r :0
   :0 :l})

(defn rotate [direction piece]
  (let [rotation (get piece :rotation)
        next-rotations (case direction
                         :clockwise rotation-clockwise
                         :counterclockwise rotation-counterclockwise
                         rotate-clockwise)
        next-rotation (get next-rotations rotation)]
    (assoc piece :rotation next-rotation)))

(defn move-piece [[x y] state]
  (let [{stack :stack
         piece :piece} state
        next-piece (-> piece
                       (update-in [:x] + x)
                       (update-in [:y] + y))]
    (if (can-place? stack next-piece)
      (assoc state :piece next-piece)
      state)))

(defn get-wall-kicks [direction piece]
  (let [rotation (get piece :rotation)
        i-piece? (= :i (get-in piece [:piece :name]))
        clockwise? (= direction :clockwise)]
    (get
     (case [i-piece? clockwise?]
       [true true] tetrominoes/wall-kick-i-clockwise
       [true false] tetrominoes/wall-kick-i-counterclockwise
       [false true] tetrominoes/wall-kick-clockwise
       [false false] tetrominoes/wall-kick-counterclockwise)
     rotation)))

(defn rotate-piece [direction state]
  (let [{stack :stack
         piece :piece} state
        rotated-piece (rotate direction piece)
        {piece-x :x
         piece-y :y} rotated-piece
        wall-kicks (get-wall-kicks direction rotated-piece)
        next-piece (->> wall-kicks
                        (map (fn [[kick-x kick-y]]
                               (assoc rotated-piece
                                      :x (+ piece-x kick-x)
                                      :y (- piece-y kick-y))))
                        (filter (fn [piece] (can-place? stack piece)))
                        (first)
                        (#(or %1 piece)))]
    (assoc state :piece next-piece)))

(defn get-lowest-y [state]
  (let [{stack :stack
         piece :piece} state
        last-y (->> (range (get piece :y) (count stack))
                    (filter #(not (can-place? stack (assoc piece :y (+ 1 %1)))))
                    (first))]
    last-y))

(defn drop-piece [state]
  (assoc-in state [:piece :y] (get-lowest-y state)))

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
        stack-wo-lines (filterv #(not-every? some? %1) stack)
        removed-lines-count (- field-height (count stack-wo-lines))
        new-empty-lines (vec (repeat removed-lines-count (vec (repeat field-width nil))))
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

(defn place-ghost-piece [state]
  (let [{stack :stack
         piece :piece} state
        lowest-y (get-lowest-y (update-in state [:piece :y] inc))
        ghost-piece (-> piece
                        (assoc :y lowest-y)
                        (assoc-in [:piece :name] :g))
        next-stack (reduce
                    (fn [stack [y x]] (assoc-in stack [y x] :g))
                    stack
                    (piece->coords ghost-piece))]
    (assoc state :stack next-stack)))

(defn next-piece-cycle [state]
  (-> state
      (place-piece)
      (remove-filled-lines)
      (use-next-piece)
      (check-game-over)))

(defn fall [state]
  (let [next-state (move-piece [0 1] state)
        moved? (not= state next-state)]
    (if moved?
      next-state
      (next-piece-cycle state))))

(defn hard-drop [state]
  (-> state
      (drop-piece)
      (next-piece-cycle)))
