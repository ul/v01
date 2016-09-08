(ns v01.view
  (:require [v01.state :as state]))

(defn App []
  [:div
   {:style {:display "flex"
            :align-items "center"
            :justify-content "space-between"
            :flex-wrap "wrap"
            :width "100%"
            :height "100%"}}
   [:div
    [:img {:src "round-structures.png"}]]
   [:div {:style {:width "1rem"}}]
   [:div
    {:style {:flex 1
             :min-width "200px"}}
    [:label
     "Freq"
     [:input
      {:type      "range"
       :min       20
       :max       1760
       :value     @state/freq
       :ev-change #(reset! state/freq (js/parseFloat (.. % -target -value)))
       :style     {:width "100%"}}]]]])