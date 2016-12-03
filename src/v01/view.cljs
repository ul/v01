(ns v01.view
  (:require [v01.state :as state]
            [v01.widgets.core :refer [Widget]]))

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
      {:type     "range"
       :min      20
       :max      1760
       :value    @state/freq
       :ev-input #(->> % (input-float) (reset! state/freq))}]]
    [Widget state/volume]]])