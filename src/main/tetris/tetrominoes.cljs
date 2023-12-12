(ns tetris.tetrominoes)

(def l-piece
  {:name :l
   :0 [[1 0 0]
       [1 1 1]
       [0 0 0]]
   :r [[0 1 1]
       [0 1 0]
       [0 1 0]]
   :2 [[0 0 0]
       [1 1 1]
       [0 0 1]]
   :l [[0 1 0]
       [0 1 0]
       [1 1 0]]})

(def j-piece
  {:name :j
   :0 [[0 0 2]
       [2 2 2]
       [0 0 0]]
   :r [[0 2 0]
       [0 2 0]
       [0 2 2]]
   :2 [[0 0 0]
       [2 2 2]
       [2 0 0]]
   :l [[2 2 0]
       [0 2 0]
       [0 2 0]]})

(def s-piece
  {:name :s
   :0 [[0 3 3]
       [3 3 0]
       [0 0 0]]
   :r [[0 3 0]
       [0 3 3]
       [0 0 3]]
   :2 [[0 0 0]
       [0 3 3]
       [3 3 0]]
   :l [[3 0 0]
       [3 3 0]
       [0 3 0]]})

(def t-piece
  {:name :t
   :0 [[0 4 0]
       [4 4 4]
       [0 0 0]]
   :r [[0 4 0]
       [0 4 4]
       [0 4 0]]
   :2 [[0 0 0]
       [4 4 4]
       [0 4 0]]
   :l [[0 4 0]
       [4 4 0]
       [0 4 0]]})

(def z-piece
  {:name :z
   :0 [[5 5 0]
       [0 5 5]
       [0 0 0]]
   :r [[0 0 5]
       [0 5 5]
       [0 5 0]]
   :2 [[0 0 0]
       [5 5 0]
       [0 5 5]]
   :l [[0 5 0]
       [5 5 0]
       [5 0 0]]})

(def o-piece
  {:name :o
   :0 [[6 6]
       [6 6]]
   :r [[6 6]
       [6 6]]
   :2 [[6 6]
       [6 6]]
   :l [[6 6]
       [6 6]]})

(def i-piece
  {:name :i
   :0 [[0 0 0 0]
       [7 7 7 7]
       [0 0 0 0]
       [0 0 0 0]]
   :r [[0 0 7 0]
       [0 0 7 0]
       [0 0 7 0]
       [0 0 7 0]]
   :2 [[0 0 0 0]
       [0 0 0 0]
       [7 7 7 7]
       [0 0 0 0]]
   :l [[0 7 0 0]
       [0 7 0 0]
       [0 7 0 0]
       [0 7 0 0]]})

(def srs-items [l-piece
                j-piece
                s-piece
                t-piece
                z-piece
                o-piece
                i-piece])

(def wall-kick-0->R
  [[0 0]
   [-1 0]
   [-1 -1]
   [0 2]
   [-1 2]])
(def wall-kick-R->2
  [[0 0]
   [1 0]
   [1 1]
   [0 -2]
   [1 -2]])
(def wall-kick-2->L
  [[0 0]
   [1 0]
   [1 -1]
   [0 2]
   [1 2]])
(def wall-kick-L->0
  [[0 0]
   [-1 0]
   [-1 1]
   [0 -2]
   [-1 -2]])

(def wall-kick-R->0
  [[0 0]
   [1 0]
   [1 -1]
   [0 2]
   [1 2]])
(def wall-kick-2->R
  [[0 0]
   [-1 0]
   [-1 1]
   [0 -2]
   [-1 -2]])
(def wall-kick-L->2
  [[0 0]
   [-1 0]
   [-1 -1]
   [0 2]
   [-1 2]])
(def wall-kick-0->L
  [[0 0]
   [1 0]
   [1 1]
   [0 -2]
   [1 -2]])

(def wall-kick-i-0->R
  [[0 0]
   [-2 0]
   [1 0]
   [-2 1]
   [1 -2]])
(def wall-kick-i-R->2
  [[0 0]
   [-1 0]
   [2 0]
   [-1 -2]
   [2 1]])
(def wall-kick-i-2->L
  [[0 0]
   [2 0]
   [-1 0]
   [2 -1]
   [-1 2]])
(def wall-kick-i-L->0
  [[0 0]
   [1 0]
   [-2 0]
   [1 2]
   [-2 -1]])

(def wall-kick-i-R->0
  [[0 0]
   [2 0]
   [-1 0]
   [2 1]
   [-1 -2]])
(def wall-kick-i-2->R
  [[0 0]
   [1 0]
   [-2 0]
   [1 -2]
   [-2 1]])
(def wall-kick-i-L->2
  [[0 0]
   [-2 0]
   [1 0]
   [-2 -1]
   [1 2]])
(def wall-kick-i-0->L
  [[0 0]
   [-1 0]
   [2 0]
   [-1 2]
   [2 -1]])

(def wall-kick-clockwise
  {:0 wall-kick-0->R
   :r wall-kick-R->2
   :2 wall-kick-2->L
   :l wall-kick-L->0})

(def wall-kick-counterclockwise
  {:0 wall-kick-R->0
   :r wall-kick-2->R
   :2 wall-kick-L->2
   :l wall-kick-0->L})

(def wall-kick-i-clockwise
  {:0 wall-kick-i-0->R
   :r wall-kick-i-R->2
   :2 wall-kick-i-2->L
   :l wall-kick-i-L->0})

(def wall-kick-i-counterclockwise
  {:0 wall-kick-i-R->0
   :r wall-kick-i-2->R
   :2 wall-kick-i-L->2
   :l wall-kick-i-0->L})