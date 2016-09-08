(ns v01.core
  (:require [carbon.vdom :as vdom]
            [v01.sync :as sync]
            [v01.view :as view]))

(enable-console-print!)

(vdom/mount js/document.body [view/App])

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
