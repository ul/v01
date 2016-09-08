(ns v01.control
  #?(:cljs (:require [carbon.rx :as rx :include-macros true])))

(def initial-state
  {:freq 440.0})

(defonce state (#?(:clj atom :cljs rx/cell) initial-state))