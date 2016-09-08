(ns v01.view
  (:require [v01.state :as state]))

(defn input-float [e]
  (js/parseFloat (.. e -target -value)))

(defn App []
  [:.app
   [:div
    [:img {:src "round-structures.png"}]]
   [:.tracks
    [:label
     "Freq"
     [:input.slider
      {:type      "range"
       :min       20
       :max       1760
       :value     @state/freq
       :ev-change #(->> % (input-float) (reset! state/freq))}]]]])