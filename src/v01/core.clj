(ns v01.core
  "Where things are assembled and start making noise"
  (:require [v01.sandbox :as sandbox])
  (:gen-class))

(defn -main [& args]
  (sandbox/play))
