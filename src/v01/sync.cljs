(ns v01.sync
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [taoensso.sente :as sente]
            [v01.control :as control]))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/ws"                     ; Note the same path as before
                                  {:type :auto              ; e/o #{:auto :ajax :ws}
                                   })]
  (def chsk chsk)
  (def ch-chsk ch-recv)                                     ; ChannelSocket's receive channel
  (def chsk-send! send-fn)                                  ; ChannelSocket's send API fn
  (def chsk-state state)                                    ; Watchable, read-only atom
  )

(go-loop []
  (when-let [{[_ [id & data]] :event} (<! ch-chsk)]
    (case id
      ::set (reset! control/state (first data))
      nil)
    (recur)))

(add-watch chsk-state ::init
  (fn [_ _ {was-open :open?} {now-open :open?}]
    (when (and now-open (not was-open))
      (chsk-send! [::init]))))

(add-watch control/state ::send #(chsk-send! [::set %4]))
