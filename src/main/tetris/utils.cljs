(ns tetris.utils)

(defn iterate-stack [stack callback]
  (dorun
   (for [[stack-y line] (map-indexed vector stack)]
     (dorun
      (for [[stack-x piece] (map-indexed vector line)]
        (callback stack-y stack-x piece))))))