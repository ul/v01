(ns v01.instruments.drum-machine
  (:require [pink.filters :as filters]
            [pink.util :refer [mul sum]]
            [pink.envelopes :as env]
            [pink.oscillators :as osc]
            [pink.noise :as noise]))

;;; NOTE use smth like pink.simple/i to control duration
;;; TODO add documentation to each instrument

(defn kick
  ""
  [{:keys [freq lpf-freq lpf-q release]
    :or   {freq     50.0
           lpf-freq 250.0
           lpf-q    0.5
           release  0.1}}]
  (filters/biquad-lpf
    (mul
      (env/xar 0.001 release)
      (osc/blit-square freq))
    lpf-freq
    lpf-q))

(defn soft-snare
  ""
  [{:keys [freq
           hpf-freq hpf-q
           sine-amp sine-a sine-d sine-s sine-r
           noise-amp noise-a noise-d noise-s noise-r]
    :or   {freq      110.0
           hpf-freq  10000.0
           hpf-q     0.5
           sine-amp  0.5
           sine-a    0.005
           sine-d    0.01
           sine-s    0.2
           sine-r    0.03
           noise-amp 0.5
           noise-a   0.02
           noise-d   0.03
           noise-s   0.15
           noise-r   0.05}}]
  (filters/biquad-hpf
    (sum
      (mul
        sine-amp
        (env/adsr sine-a sine-d sine-s sine-r)
        (osc/sine freq))
      (mul
        noise-amp
        (env/adsr noise-a noise-d noise-s noise-r)
        (noise/white-noise)))
    hpf-freq
    hpf-q))

(defn hard-snare
  ""
  [{:keys [freq
           hpf-freq hpf-q
           sine-amp sine-a sine-d sine-s sine-r
           ]
    :or   {freq     110.0
           hpf-freq 300.0
           hpf-q    0.5
           sine-amp 1.0
           sine-a   0.01
           sine-d   0.1
           sine-s   0.0
           sine-r   0.0}}]
  (filters/biquad-hpf
    (sum
      (mul
        sine-amp
        (env/adsr sine-a sine-d sine-s sine-r)
        (osc/sine freq))
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
    hpf-freq
    hpf-q))

(defn hi-hat
  ""
  [{:keys [hpf-freq hpf-q a d s r]
    :or   {hpf-freq 10000.0
           hpf-q    0.5
           a        0.01 d 0.05 s 0.2 r 0.05}}]
  (filters/biquad-hpf
    (mul
      (env/adsr a d s r)
      (noise/white-noise))
    hpf-freq
    hpf-q))