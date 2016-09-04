(ns v01.core
  "Where things are assembled and start making noise"
  (:require [v01.web :as web]
            [v01.sandbox :as sandbox])
  (:gen-class))

(defn -main [& args]
  (web/start)
  (sandbox/play-sine))
