(ns v01.sandbox
  "Noise incubator"
  (:require [pink.simple :as pink]
            [pink.oscillators :as osc]
            [pink.envelopes :as env]
            [pink.util :as util]
            [pink.event :refer [event]]
            [pink.space :as space]
            [score.core :as score]
            [v01.instrument.drum-machine :as drum]
            [v01.control :as control]
            [v01.util :refer [sco->events]]))

;;; Start the engine!

(pink/start-engine)

;;; Probably this will be helpful one day

(def sq5 (Math/sqrt 5))
(def phi (/ (+ sq5 1) 2))
(def psi (/ (- sq5 1) 2))

(defn fib [n]
  (/ (-
       (Math/pow phi n)
       (Math/pow psi n))
     sq5))

(defn deg->rad [x]
  (/ (* x Math/PI) 180.0))

(defn rrand [from to]
  (+ from (rand (- to from))))

;;;

(def xk [[drum/kick 0.0 0.1]])
(def yk [[drum/kick 0.0 0.1] [drum/soft-snare 0.25 0.1]])
(def zk (score/convert-timed-score
          [0.0 xk
           0.5 yk]))

(def score
  [:meter 4 4
   0 xk zk
   0.25 xk
   1 yk])

(defn play-score [score]
  (-> score
      (score/convert-measured-score)
      (sco->events)
      (pink/add-audio-events)))

(comment
  (play-score score))

(comment
  (pink/clear-engine))

;;; Write to disk example

(comment
  (do
    (require '[pink.engine :as engine])
    (import '[java.io File])

    (let [e (engine/engine-create :nchnls 2)]
      ;; rock'n'roll here!
      (->> score
           (score/convert-measured-score)
           (sco->events)
           (engine/audio-events e)
           (engine/engine-add-events e))
      (engine/engine->disk e (str (System/getProperty "user.home")
                                  File/separator "test.wav"))
      (engine/engine-clear e)
      (engine/engine-kill-all))))

;;; Example of external control

(defn freq-gen []
  (let [out ^doubles (util/create-buffer)]
    (util/generator
      [] []
      (do
        (aset out int-indx (double (get @control/state :freq)))
        (util/gen-recur))
      (yield out))))

(def gen1 (osc/sine2 (freq-gen)))

(defn play-sine []
  (pink/add-afunc gen1))

;;; One of the first experiment's wreckage

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
