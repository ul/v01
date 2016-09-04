(ns v01.core
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [carbon.vdom :as vdom]
            [carbon.rx :as rx]
            [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente :as sente :refer (cb-success?)]))

(enable-console-print!)

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk"                   ; Note the same path as before
                                  {:type :auto              ; e/o #{:auto :ajax :ws}
                                   })]
  (def chsk chsk)
  (def ch-chsk ch-recv)                                     ; ChannelSocket's receive channel
  (def chsk-send! send-fn)                                  ; ChannelSocket's send API fn
  (def chsk-state state)                                    ; Watchable, read-only atom
  )

(defonce freq (rx/cell 440))
(add-watch freq :send
  (fn [_ _ _ freq]
    (chsk-send! [::freq freq])))

(defn App []
  [:div
   [:h1 "Hello, v01!"]
   [:input
    {:type      "range"
     :min       20
     :max       1760
     :value     @freq
     :ev-change #(reset! freq (js/parseFloat (.. % -target -value)))
     :style     {:width "100%"}}]])

(vdom/mount js/document.body [App])

(go-loop []
  (when-let [{:keys [event]} (<! ch-chsk)]
    (js/console.log event)
    (recur)))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
