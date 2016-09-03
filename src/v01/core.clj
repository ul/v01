(ns v01.core
  "Where things are assembled and start making noise"
  (:require [pink.simple :as pink]
            [pink.oscillators :as osc])
  (:gen-class))

(pink/start-engine)

(def gen1 (osc/sine 440))

(pink/add-afunc gen1)

(Thread/sleep 5000)

(pink/remove-afunc gen1)

(pink/stop-engine)

(defn -main [& args]
  )
