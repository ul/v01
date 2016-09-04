(ns v01.web
  (:require [immutant.web :as web]
            [compojure.core :refer [defroutes GET POST]]
            ring.middleware.resource
            ring.middleware.keyword-params
            ring.middleware.params
            [clojure.java.io :as io]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.immutant :refer (get-sch-adapter)]
            [clojure.core.async :as async]
            [v01.state :as state]))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]

  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)                                     ; ChannelSocket's receive channel
  (def chsk-send! send-fn)                                  ; ChannelSocket's send API fn
  (def connected-uids connected-uids)                       ; Watchable, read-only atom
  )

(defroutes app
  (GET "/" [] (io/resource "public/index.html"))
  (GET "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req)))

(defn start []
  (web/run-dmc (-> app
                   (ring.middleware.resource/wrap-resource "public")
                   ring.middleware.keyword-params/wrap-keyword-params
                   ring.middleware.params/wrap-params)
               {:host "0.0.0.0"}))

;;;;

(async/go-loop []
  (when-let [{:keys [uid] [id & data :as event] :event} (async/<! ch-chsk)]
    (println event)
    (case id
      :v01.core/freq (reset! state/freq (double (first data)))
      nil)
    (chsk-send! uid event)
    (recur)))
