(ns v01.util
  "To fix ancestors' faults..."
  (:require [pink.simple :as simple]
            [pink.engine :as engine]
            [pink.event :refer [event]]
            [pink.util :refer [with-duration]])
  (:import (clojure.lang IDeref)))

(defn deref!*! [x]
  (if (instance? IDeref x)
    (deref x)
    x))

(defn apply!*! [func args]
  (->> args
       (map deref!*!)
       (apply func)))

(defn apply-afunc-with-dur
  "Applies an afunc to args, wrapping results with (with-duration dur)."
  [e afunc dur args]
  (engine/engine-add-afunc e
    (with-duration (double dur)
      (apply!*! afunc args))))

(defn i
  "Csound style note events: audio-func, start, dur, & args.
  Wraps into an event that will call audio-func with args, and wrap
  with with-duration call with dur. Most likely used in conjunction
  with add-audio-events so that generated afuncs will be added to
  to an engine."
  ([e [afunc start dur & args]]
   (event apply-afunc-with-dur start e afunc dur args)))

(defn sco->events
  "Convert SCO formatted note lists into events by applying i to all notes.
  SCO format follows Csound style note events: audio-func, start, dur, & args."
  ([notes] (sco->events simple/engine notes))
  ([e notes]
   (map #(i e %) notes)))