(ns v01.web
  "Poetry of control written on webs riding wind."
  (:require [immutant.web :as web]
            [compojure.core :refer [defroutes GET POST]]
            ring.middleware.resource
            ring.middleware.keyword-params
            ring.middleware.params
            [clojure.java.io :as io]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.immutant :refer (get-sch-adapter)]
            [clojure.core.async :as async]
            [v01.control :as control]))

;; Here Immutant webserver appears on the stage. He is fast-handed and talkative, but shy and like to serve people.

;; His friend Sente helps him to speak with Browser in EDN, the language of Clojure structures.
(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]

  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk "ChannelSocket's receive channel." ch-recv)
  (def chsk-send! "ChannelSocket's send API fn." send-fn)
  (def connected-uids
    "Watchable, read-only atom with structure:
    {:ws   #{UIDs of clients connected by WebSocket}
     :ajax #{UIDs of clients connected by long-polling AJAX fallback}
     :any  #{All client UIDs}}"
    connected-uids))

;; Say a right route with your Browser, and Immutant will bring you to desired place.
(defroutes routes
  (GET "/" [] (io/resource "public/index.html"))
  (GET "/ws" req (ring-ajax-get-or-ws-handshake req))
  (POST "/ws" req (ring-ajax-post req)))

(def app
  "Middlewares pre-process request and post-process response for every route."
  (-> routes
      (ring.middleware.resource/wrap-resource "public")     ; serve static files from resources/public/
      ring.middleware.keyword-params/wrap-keyword-params    ; transform query parameters keys to Clojure keywords
      ring.middleware.params/wrap-params))                  ; parse parameters

(defn start
  "Start server with `app` handler configured earlier. Bind to all interfaces (listen entire world)."
  []
  (web/run app {:host "0.0.0.0"}))

;;; Sync `v01.control/state` with clients: when changed, send to each and every client; and receive updates from clients.

(defn broadcast-state
  "Given previous and current state, send the latter as `:v01.sync/set` event to all clients if changed."
  [_ _ prev state]
  (when (not= prev state)
    (doseq [uid (get @connected-uids :any)]
      (chsk-send! uid [:v01.sync/set state]))))

;; Watch `v01.control/state` to broadcast updates
(add-watch control/state ::send broadcast-state)

;; Listen clients and update state on `:v01.sync/set` event
(async/go-loop []
  (when-let [{:keys [uid] [id & data] :event} (async/<! ch-chsk)]
    (case id
      :v01.sync/set (reset! control/state (first data))
      nil)
    (chsk-send! uid [::ok])
    (recur)))
