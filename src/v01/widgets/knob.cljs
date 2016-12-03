(ns v01.widgets.knob)

(defn linear-map [x x-min x-max y-min y-max]
  (-> (- y-max y-min)
      (/ (- x-max x-min))
      (* (- x x-min))
      (+ y-min)))

(defn knob [{:keys [label value set-value min max size]
             :or {min 0.0
                  max 100.0
                  size 1.0}}]
  (let [scale (* 0.24 size)
        ellispse-id (str (gensym))
        mask-id (str (gensym))]
    [:svg
     {:version     "1.1"
      :xmlns       "http://www.w3.org/2000/svg"
      :xmlns:xlink "http://www.w3.org/1999/xlink"
      :width (* 104 scale)
      :height (* 104 scale)}
     [:title "Knob"]
     [:defs
      [:ellipse
       {:id ellispse-id
        :cx 52
        :cy 52
        :rx 50
        :ry 50}]]
     [:g
      {:stroke       "none"
       :stroke-width 4
       :fill         "none"
       :fill-rule    "evenodd"
       :transform   (str "scale(" scale ")")}
      [:ellipse
       {:stroke "#000000"
        :cx     52
        :cy     52
        :rx     50
        :ry     50}]
      [:g
       {:fill-rule "evenodd"}
       [:mask
        {:id   mask-id
         :fill "#ffffff"}
        [:use
         {:xlink:href (str "#" ellispse-id)}]]
       [:ellipse
        {:fill      "#000000"
         :transform (str "rotate(" (linear-map value min max -120 200) " 52 52)")
         :mask      (str "url(#" mask-id ")")
         :cx        30
         :cy        30
         :rx        8
         :ry        8}]]]]))