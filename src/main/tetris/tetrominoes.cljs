(ns tetris.tetrominoes)

(def i
  [[1]
   [1]
   [1]
   [1]])
(def z
  [[2 2 0]
   [0 2 2]])
(def s
  [[0 3 3]
   [3 3 0]])
(def o
  [[4 4]
   [4 4]])
(def t
  [[5 5 5]
   [0 5 0]])
(def j
  [[6 6 6]
   [0 0 6]])
(def l
  [[7 7 7]
   [7 0 0]])

(def items
  [i z s o t j l])

(def l-piece [[1 0 0]
              [1 1 1]
              [0 0 0]])
(def j-piece [[0 0 2]
              [2 2 2]
              [0 0 0]])
(def s-piece [[0 3 3]
              [3 3 0]
              [0 0 0]])
(def t-piece [[0 4 0]
              [4 4 4]
              [0 0 0]])
(def z-piece [[5 5 0]
              [0 5 5]
              [0 0 0]])
(def o-piece [[6 6]
              [6 6]])
(def i-piece [[0 0 0 0]
              [7 7 7 7]
              [0 0 0 0]
              [0 0 0 0]])
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
   [-1 1]
   [0 -2]
   [-1 -2]])

(def wall-kick-R->2
  [[0 0]
   [1 0]
   [1 -1]
   [0 2]
   [1 2]])

(def wall-kick-2->L
  [[0 0]
   [1 0]
   [1 1]
   [0 -2]
   [1 -2]])

(def wall-kick-L->0
  [[0 0]
   [-1 0]
   [-1 -1]
   [0 2]
   [-1 2]])

(def wall-kick-i-0->R
  [[0 0]
   [-2 0]
   [1 0]
   [-2 -1]
   [1 2]])

(def wall-kick-i-R->2
  [[0 0]
   [-1 0]
   [2 0]
   [-1 2]
   [2 -1]])

(def wall-kick-i-2->L
  [[0 0]
   [2 0]
   [-1 0]
   [2 1]
   [-1 -2]])

(def wall-kick-i-L->0
  [[0 0]
   [1 0]
   [-2 0]
   [1 -2]
   [-2 1]])