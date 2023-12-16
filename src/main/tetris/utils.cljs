(ns tetris.utils)

(defn iterate-stack [stack callback]
  (dorun
   (for [[stack-y line] (map-indexed vector stack)]
     (dorun
      (for [[stack-x piece] (map-indexed vector line)]
        (callback stack-x stack-y piece))))))

(defn empty-line? [line] (every? nil? line))

(defn filled-line? [line] (every? keyword? line))

