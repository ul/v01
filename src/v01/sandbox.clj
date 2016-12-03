(ns v01.sandbox
  "Noise incubator"
  (:require [pink.simple :as pink]
            [pink.oscillators :as osc]
            [pink.envelopes :as env]
            [pink.util :as util :refer [sum mul]]
            [pink.event :refer [event]]
            [pink.filters :as filters]
            [pink.space :as space]
            [score.core :as score]
            [carbon.rx :as rx]
            [v01.midi :as midi :refer [midi->linear]]
            [v01.instruments.drum-machine :as drum]
            [v01.control :as control]
            [v01.util :refer [atom->afn]]))

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

(declare kick hi-hat snare kick-params hat-params snare-params)

(def xk [[kick 0.0 0.1 kick-params]])
(def yk [[kick 0.0 0.1 kick-params] [hi-hat 0.25 0.1 hat-params]])
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
      (pink/sco->events)
      (pink/add-audio-events)))

(comment
  (play-score
    [:meter 3 5
     0 (score/gen-notes
         (cycle [kick hi-hat snare])
         (range 1000)
         0.1
         (cycle [kick-params hat-params snare-params]))]))

(comment
  (pink/set-tempo 90))

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
           (pink/sco->events)
           (engine/audio-events e)
           (engine/engine-add-events e))
      (engine/engine->disk e (str (System/getProperty "user.home")
                                  File/separator "test.wav"))
      (engine/engine-clear e)
      (engine/engine-kill-all))))

;;;

(defn update-cell [c _ old new]
  (when (not= old new)
    (reset! c new)))

;; TODO make native wrapper in carbon.rx
(defn atom->cell [a]
  (let [c (rx/cell @a)]
    (add-watch a c update-cell)
    c))

(midi/list-devices)
(defonce _2 (midi/device-debug "MIDI Mix"))

(defonce midim (midi/create-manager))
(defonce akai (midi/add-virtual-device midim "slider/knobs 0"))

(defonce _1
           (do
             (midi/bind-device midim "MIDI Mix" "slider/knobs 0")
             (midi/bind-key-processor akai 0)))
(def get-cc (partial midi/get-cc-atom akai 0))

(def knob-codes
  [[16 17 18]
   [20 21 22]
   [24 25 26]
   [28 29 30]
   [46 47 48]
   [50 51 52]
   [54 55 56]
   [58 59 60]])
(def slider-codes [19 23 27 31 49 53 57 61 62])
(def button-codes
  [[1 3]
   [4 6]
   [7 9]
   [10 12]
   [13 15]
   [16 18]
   [19 21]
   [22 24]
   [25 26 27]])

(defonce knobs (mapv (partial mapv (comp atom->cell get-cc)) knob-codes))
(defonce sliders (mapv (comp atom->cell get-cc) slider-codes))

(defn knob [column row]
  (get-in knobs [(dec column) (dec row)]))

(defn slider [column]
  (get sliders (dec column)))

(defn wrap-pan [f]
  (fn [params]
    (space/pan (f params) (get params :pan 0.0))))

(def kick (wrap-pan drum/kick))
(def hi-hat (wrap-pan drum/hi-hat))
(def snare (wrap-pan drum/soft-snare))

(defonce kick-base-freq (rx/rx (midi->linear @(slider 1) 20.0 120.0)))
(defonce kick-lpf-freq (rx/rx (midi->linear @(knob 1 2) 100.0 500.0)))
(defonce kick-release (rx/rx (midi->linear @(knob 1 3) (/ 1.0 32.0) 1.0)))
(defonce kick-pan (rx/rx (midi->linear @(knob 1 1) -1.0 1.0)))

(defonce kick-params (rx/rx {:freq          @kick-base-freq
                             :kick-lpf-freq @kick-lpf-freq
                             :release       @kick-release
                             :pan           @kick-pan}))

(defonce hat-hpf-freq (rx/rx (midi->linear @(knob 2 2) 5000.0 15000.0)))
(defonce hat-hpf-q (rx/rx (midi->linear @(knob 2 3) 0.1 5.0)))
(defonce hat-a (rx/rx (midi->linear @(slider 2) 0.005 0.02)))
(defonce hat-d (rx/rx (midi->linear @(knob 3 1) 0.01 0.1)))
(defonce hat-s (rx/rx (midi->linear @(knob 3 2) 0.1 0.5)))
(defonce hat-r (rx/rx (midi->linear @(knob 3 3) 0.01 0.1)))
(defonce hat-pan (rx/rx (midi->linear @(knob 2 1) -1.0 1.0)))

(defonce hat-params (rx/rx {:hpf-freq @hat-hpf-freq
                            :hpf-q    @hat-hpf-q
                            :a        @hat-a
                            :d        @hat-d
                            :s        @hat-s
                            :r        @hat-r
                            :pan      @hat-pan}))

(defonce snare-freq (rx/rx (midi->linear @(slider 3) 50.0 220.0)))
(defonce snare-hpf-freq (rx/rx (midi->linear @(knob 4 2) 5000.0 15000.0)))
(defonce snare-hpf-q (rx/rx (midi->linear @(knob 4 3) 0.1 5.0)))
(defonce snare-amp (rx/rx (midi->linear @(knob 5 1) 0.01 1.0)))
(defonce snare-a (rx/rx (midi->linear @(knob 5 2) 0.001 0.01)))
(defonce snare-d (rx/rx (midi->linear @(knob 5 3) 0.005 0.5)))
(defonce snare-s (rx/rx (midi->linear @(slider 5) 0.1 0.5)))
(defonce snare-r (rx/rx (midi->linear @(knob 6 1) 0.005 0.06)))
(defonce snare-pan (rx/rx (midi->linear @(knob 4 1) -1.0 1.0)))

(defonce snare-params (rx/rx {:freq     @snare-freq
                              :hpf-freq @snare-hpf-freq
                              :hpf-q    @snare-hpf-q
                              :sine-amp @snare-amp
                              :sine-a   @snare-a
                              :sine-d   @snare-d
                              :sine-s   @snare-s
                              :sine-r   @snare-r
                              :pan      @snare-pan}))


;;; Example of external control

(comment

  (pink/add-audio-events (event kick 0.0 kick-params))

  (reset! (:key-bindings akai) {})

  (pink/add-audio-events (event hi-hat 0.0 hat-params))

  (midi/bind-key akai 1
                 (fn [_]
                   (pink/add-audio-events (event kick 0.0 kick-params)))
                 (fn [_]))

  (midi/bind-key akai 4
                 (fn [_]
                   (pink/add-audio-events
                     (event hi-hat 0.0 hat-params)))
                 (fn [_]))

  )

(def state (atom->cell control/state))

(def freq-cell (rx/rx (get @state :freq)))

(def gen1 (osc/sine2 (atom->afn freq-cell identity)))

(def gen11
  (space/pan
    (osc/sine2
      (osc/sine2 (atom->afn (get-cc 19) #(midi/midi->linear % 0.1 440.0)))
      (osc/sine2 (atom->afn (get-cc 23) #(midi/midi->linear % 0.1 440.0))))
    0.0))

(def gens (vec (for [code slider-codes]
                 (mul 0.125
                      (osc/sine2
                        (atom->afn
                          (get-cc code)
                          #(midi/midi->linear % 0.1 880.0)))
                      ))))

(def gen22
  (space/pan
    (apply sum gens)
    0.0))

(comment
  (pink/add-afunc gen22)
  (pink/remove-afunc gen22))

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
