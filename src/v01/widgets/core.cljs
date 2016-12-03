(ns v01.widgets.core
  [:require v01.widgets.knob])

(defmulti widget :type)

(defmethod widget :knob [m] (v01.widgets.knob/knob m))

(defn Widget [rx]
  [widget (assoc @rx :set-value #(swap! rx assoc :value %))])