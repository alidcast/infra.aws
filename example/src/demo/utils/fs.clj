(ns demo.utils.fs
  "File-system utilities."
  (:refer-clojure :exclude [resource])
  (:require [clojure.java.io :as io]))

(defn resource
  "Read edn file, `path` is relative to 'resource/demo' dir, throws error if not found."
  [path]
  (if-let [r (io/resource (str "demo/" path))]
    (slurp r)
    (throw (RuntimeException.
            (str "Resource \"" path "\" not found.")))))
