(ns v01.util
  "To fix ancestors' faults..."
  (:require [pink.util :as util :refer [with-duration]])
  (:import (java.util Arrays)))

(defn atom->afn
  [source-atom transform]
  (let [v @source-atom
        out ^doubles (util/create-buffer v)
        cur-val (atom v)]
    (fn []
      (let [v @source-atom]
        (when (not= @cur-val v)
          (reset! cur-val v)
          (Arrays/fill out (double (transform v)))))
      out)))
