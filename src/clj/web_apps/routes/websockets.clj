(ns web-apps.routes.websockets
  (:require
    [clojure.tools.logging :as log]
    [chord.http-kit :refer [wrap-websocket-handler]]
    [web-apps.db.core :as db]
    [clojure.core.async :as a]))

(def clients "A clojure.core.async/chan." (a/chan))

(def server "A clojure.core.async/mult." (a/mult clients))

(defmulti on-event-receive
          (fn [client [event-id]] event-id))

(defn sync-db [client db]
  (a/go
    (a/>! client [:web-apps.websockets/server>clients db])))

(defmethod on-event-receive ::client>server
  [client [_ tx]]
  (db/transact tx)
  (sync-db clients (db/db)))

(defmethod on-event-receive nil
  [client [_ tx]])

(defn client>server [client]
  (sync-db client (db/db))
  (a/tap server client)
  (a/go-loop [event (:message (a/<! client))]
    (when event
      (on-event-receive client event)
      (recur (:message (a/<! client))))))

(def websocket-routes
  ["/ws" {:get        (comp client>server :ws-channel)
          :middleware [wrap-websocket-handler]}])

