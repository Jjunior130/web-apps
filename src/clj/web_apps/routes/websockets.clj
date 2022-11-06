(ns web-apps.routes.websockets
  (:require
    [re-frame.core :as rf]
    [chord.http-kit :refer [wrap-websocket-handler]]
    [web-apps.db.core :as db]
    [clojure.core.async :as a]))

(def clients "A clojure.core.async/chan." (a/chan))

(def server "A clojure.core.async/mult." (a/mult clients))

(rf/reg-sub
  ::session-id
  #(:session-id %))

(defmulti on-event-receive
  (fn [client [event-id]] event-id))

(defmethod on-event-receive ::client>server
  [client [_ tx]]
  (db/transact tx)
  (db/sync-db clients))

(defmethod on-event-receive nil
  [client [_ tx]])

(defn client>server [[client session-id]]
  (a/go (a/>! client [:web-apps.websockets/init-server>clients session-id]))
  (db/transact [{:session-id session-id}])
  (db/sync-db client)
  (a/tap server client)
  (a/go-loop [event (:message (a/<! client))]
    (when event
      (on-event-receive client event)
      (recur (:message (a/<! client))))))

(def websocket-routes
  ["/ws" {:get        (comp client>server (juxt :ws-channel :session/key))
          :middleware [wrap-websocket-handler]}])

