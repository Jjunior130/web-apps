(ns web-apps.websockets
  (:require [cognitect.transit :as t]
            [taoensso.timbre :as log]
            [re-frame.core :as rf]
            [kee-frame.core :as kf]))

(def json-reader (t/reader :json))
(def json-writer (t/writer :json))

(kf/reg-event-db
  ::client>server
  (fn [{socket :client-socket :as db} [msg]]
    (if socket
      (.send socket (t/write json-writer {:message msg}))
      (throw (js/Error. "Websocket is not available!")))
    db))

(kf/reg-event-db
  ::server>clients
  (fn [db [message]]
    (update db :messages #(vec (take-last 10 (conj % message))))))

(kf/reg-event-db
  ::reg-server>clients
  (fn [{socket :client-socket :as db} _]
    (if socket
      (do
        (set! (.-onmessage socket)
              #(rf/dispatch
                 [::server>clients
                  (->> % .-data (t/read json-reader) :message)]))
        (println "Websocket connection established with: " (.-url socket)))
      (throw (js/Error. "Websocket connection failed!")))
    db))

(kf/reg-event-db
  ::open-socket
  (fn [db [url]]
    (assoc db
      :client-socket (js/WebSocket. url))))
