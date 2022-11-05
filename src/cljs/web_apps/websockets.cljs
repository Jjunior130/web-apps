(ns web-apps.websockets
  (:require [cognitect.transit :as t]
            [taoensso.timbre :as log]
            [re-frame.core :as rf]
            [kee-frame.core :as kf]
            [re-posh.core :as rp]
            [chord.client :as chord]
            [cljs.core.async :as a]
            [web-apps.db :as db])
  (:require-macros
    [cljs.core.async.macros :as a]))

(rf/reg-cofx
  ::url
  (fn [cofx]
    (assoc cofx
      ::url (str (if (= "https:" (-> js/document
                                   .-location .-protocol))
                   "wss://"
                   "ws://")
              (-> js/document .-location .-host)
              "/ws"))))

(kf/reg-event-db
  ::client>server
  (fn [{server :server-socket :as db}
       [datoms]]
    (if server
      (a/go (a/>! server [:web-apps.routes.websockets/client>server datoms]))
      (throw (js/Error. "Websocket is not available!")))
    nil))

(rp/reg-event-fx
  ::server>clients
  (fn [_ [_ db]]
    (db/sync-db db)
    nil))

(defn- server>clients! [server]
  (a/go-loop []
    (if-let [db-event (:message (a/<! server))]
      (do
        (rf/dispatch
          db-event)
        (recur))
      (rf/dispatch [::disconnected false]))))

(kf/reg-event-fx
  ::disconnected
  [(rf/inject-cofx ::url)]
  (fn [{{server :server-socket
         :as    db} :db
        url         ::url
        :as         ctx}
       [reconnect?]]
    (when server
      (when server (a/close! server))
      (merge
        {:db (assoc db :server-socket nil)}
        (when reconnect?
          {::open-socket url})))))

(kf/reg-event-db
  ::reg-server>clients
  (fn [db [new-server]]
    (server>clients! new-server)
    (assoc db
      :server-socket new-server)))

(kf/reg-event-fx
  ::error
  (fn [ctx [url error]]
    (js/alert (str "Error connecting to server: " error "\n"
                "Url: " url))))

(rf/reg-fx
  ::open-socket
  (fn [url]
    (a/go
      (let [{:keys [ws-channel error]}
            (a/<! (chord/ws-ch url))]
        (if error
          (rf/dispatch [::error url error])
          (rf/dispatch [::reg-server>clients ws-channel]))))))
