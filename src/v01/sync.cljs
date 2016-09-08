(ns v01.sync
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [taoensso.sente :as sente]
            [v01.control :as control]))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk"                   ; Note the same path as before
                                  {:type :auto              ; e/o #{:auto :ajax :ws}
                                   })]
  (def chsk chsk)
  (def ch-chsk ch-recv)                                     ; ChannelSocket's receive channel
  (def chsk-send! send-fn)                                  ; ChannelSocket's send API fn
  (def chsk-state state)                                    ; Watchable, read-only atom
  )

(add-watch control/state ::send
  (fn [_ _ _ state]
    (chsk-send! [::reset state])))

(go-loop []
  (when-let [{[id & data] :event} (<! ch-chsk)]
    (case id
      ::reset (reset! control/state (first data))
      nil)
    (recur)))

