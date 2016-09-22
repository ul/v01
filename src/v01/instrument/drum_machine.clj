(ns v01.instrument.drum-machine
  (:require [pink.filters :as filters]
            [pink.util :refer [mul sum]]
            [pink.envelopes :as env]
            [pink.oscillators :as osc]
            [pink.noise :as noise]))

;;; NOTE use smth like pink.simple/i to control duration
;;; TODO add documentation to each instrument

(defn kick
  ""
  []
  (filters/biquad-lpf
    (mul
      (env/xar 0.001 0.1)
      (osc/blit-square 50.0))
    250
    0.5))

(defn soft-snare
  ""
  []
  (filters/biquad-hpf
    (sum
      (mul
        0.5
        (env/adsr 0.005 0.01 0.2 0.03)
        (osc/sine 110.0))
      (mul
        0.5
        (env/adsr 0.02 0.03 0.15 0.05)
        (noise/white-noise)))
    10000
    0.5))

(defn hard-snare
  ""
  []
  (filters/biquad-hpf
    (sum
      (mul
        1.0
        (env/adsr 0.01 0.1 0.0 0.0)
        (osc/sine 110.0))
      (mul
        0.5
        (env/adsr 0.02 0.98 0.0 0.0)
        (filters/biquad-bpf
          (noise/white-noise) 500 2.5))
      (mul
        0.4
        (env/adsr 0.01 0.25 0.0 0.0)
        (filters/biquad-bpf
          (noise/white-noise) 2000 1.0)))
    300
    0.5))

(defn hi-hat
  ""
  []
  (filters/biquad-hpf
    (mul
      (env/adsr 0.01 0.05 0.2 0.05)
      (noise/white-noise))
    10000
    0.5))