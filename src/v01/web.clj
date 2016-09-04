(ns v01.web
  (:require [immutant.web :as web]
            [immutant.web.async :as async]
            [compojure.core :refer [defroutes GET]]
            [ring.middleware.resource :refer [wrap-resource]]
            [clojure.java.io :as io]))

(defn ws [request]
  (async/as-channel request
    {:on-open    (fn [channel])
     :on-close   (fn [channel {:keys [code reason]}])
     :on-error   (fn [channel throwable])
     :on-message (fn [channel message]
                   (async/send! channel message
                                :on-success #()
                                :on-error (fn [e])))}))

(defroutes app
  (GET "/" [] (io/resource "public/index.html"))
  (GET "/ws" [] ws))

(defn start []
  (web/run (-> app
               (wrap-resource "public"))))

