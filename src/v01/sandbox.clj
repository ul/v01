(ns v01.sandbox
  "Noise incubator"
  (:require [pink.simple :as pink]
            [pink.oscillators :as osc]
            [pink.envelopes :as env]
            [pink.util :as util]))

(pink/start-engine)

;;(def gen1 (osc/sine 440))

;;(pink/add-afunc gen1)

;;(Thread/sleep 5000)

;;(pink/remove-afunc gen1)

;;(pink/stop-engine)

(defn e [] (util/with-duration 5.0 (env/adsr 0.05 0.2 0.8 1.0)))

(defn blit-pulse-fucking-awesome [freq pulse-width]
  (osc/blit-pulse
    freq

    (util/sum
      (osc/sine2 pulse-width)
      (util/mul
        0.05
        (osc/sine2 pulse-width (* 0.5 Math/PI)))
      (util/mul
        0.05
        (osc/sine2 pulse-width (* 1.0 Math/PI)))
      (util/mul
        0.05
        (osc/sine2 pulse-width (* 1.5 Math/PI)))
      )))

(defn gen2 []
  (let [e (e)]
    (util/mul e (blit-pulse-fucking-awesome 80 e))))

(defn play []
  (let [bpm 60
        tact (/ 60000 bpm)
        n 8
        step (/ tact n)]
    (future
      (dotimes [i 16]
        (let [delay (/ i 8.0)]
          (when (> (rand) 0.1)
            (future
              (Thread/sleep (* 1 step))
              (pink/add-afunc (gen2))))
          (when (> (rand) 0.1)
            (future
              (Thread/sleep (* 4 step))
              (pink/add-afunc (gen2))))
          (when (> (rand) 0.1)
            (future
              (Thread/sleep (* delay step))
              (pink/add-afunc (gen2))))
          (Thread/sleep tact))))
    (Thread/sleep (* 16 tact))
    (pink/stop-engine)))
