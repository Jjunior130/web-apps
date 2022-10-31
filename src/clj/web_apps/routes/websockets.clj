(ns web-apps.routes.websockets
  (:require
    [luminus.ws :as ws]
    [web-apps.db.core :as datomic]
    [cognitect.transit :as t]
    [clojure.tools.logging :as log]))

(def connections (atom #{}))

(defn broadcast [{:keys [data this-client
                         all-clients public?]}]
  (if public?
    (doseq [client all-clients]
      (ws/send! client data))
    (ws/send! this-client data)))

(def ws-handler-a
  {:on-connect (fn [ws]
                 (log/info "WS connect"
                           (count @connections))
                 (swap! connections conj ws))
   :on-error   (fn [ws e]
                 (log/info "WS error" e))
   :on-text    (fn [ws text]
                 (log/info "text:" text)
                 (broadcast {:data text
                             :this-client ws
                             :all-clients @connections
                             :public? true}))
   :on-close   (fn [ws status-code reason]
                 (log/info "WS close" reason)
                 (swap! connections disj ws))
   :on-bytes   (fn [ws bytes offset len]
                 (log/info "WS bytes" bytes))})

(defn ws-handler
  "The function accepts the request and passes it
  to the org.httpkit.server/with-channel macro
  provided by the HTTP Kit API. The macro creates
  a handler that accepts the request as its
  argument and binds the value of the
  :async-channel key to the second paramets
  representing the name of the channel.
  The statement following the channel name will
  be called once when the channel is created."
  [request]
  (if (ws/ws-upgrade-request? request)
    ;; websocket upgrade request
    (ws/ws-upgrade-response ws-handler-a)
    ;; normal http request
    {:status  200
     :headers {"Content-Type" "text/plain"}
     :body    (:remote-addr request)}))

(def websocket-routes
  ["/ws" {:get ws-handler}])
