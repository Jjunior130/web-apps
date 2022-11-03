(ns web-apps.routes.websockets
  (:require
    [clojure.tools.logging :as log]
    [chord.http-kit :refer [wrap-websocket-handler]]
    [web-apps.db.core :as db :refer [conn]]
    [clojure.core.async :as a]))

(def connections (atom (hash-map)))

(defmulti on-event-receive
          (fn [client [event-id]] event-id))

(defmethod on-event-receive ::client>server
  [client [_ tx]]
  (a/go
    (doseq [client (vals @connections)]
      (a/>! client [:web-apps.websockets/server>clients tx]))))

(defmethod on-event-receive nil
  [client [_ tx]])

(comment
  (deref connections))

(defn client>server [client]
  (a/go-loop [event (:message (a/<! client))]
    (when event
      (log/spy (second event))
      (on-event-receive client event)
      (recur (:message (a/<! client))))))

(defn handler [{:keys [ws-channel] session :session/key :as request}]
  (when ws-channel
    (swap! connections #(merge-with (fn [old new]
                                      (when old (a/close! old))
                                      (client>server new)
                                      new)
                                    %
                                    {session ws-channel}))
    (log/spy @connections)))


(def websocket-routes
  ["/ws" {:get        handler
          :middleware [wrap-websocket-handler]}])

