(ns web-apps.setter
  (:require [re-frame.core :as rf]
            [kee-frame.core :as kf]
            [re-posh.core :as rp]
            [web-apps.getter :as getter]
            [chord.client :as chord]
            [cljs.core.async :as a]
            [web-apps.websockets :as ws]
            [web-apps.db :as db]
            [ajax.core :as http]))

(kf/reg-event-fx
  ::change-page
  [(rf/inject-cofx ::getter/now)]
  (fn [{{:keys [session-id]} :db
        now :now} [page]]
    (rf/dispatch [::client>server [{:user [:session-id session-id]
                                    :changed now
                                    :page page}]])))

(kf/reg-chain
  ::load-home-page
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/docs"
                  :response-format (http/raw-response-format)
                  :on-failure      [:common/set-error]}})
  (fn [{:keys [db]} [_ docs]]
    {:db (assoc db :docs docs)}))

(rf/reg-event-fx
  ::load-about-page
  (constantly nil))

(kf/reg-event-fx
  ::init-client
  [(rf/inject-cofx ::getter/url)]
  (fn [{url ::getter/url} _]
    {:db {:now (js/Date.)}
     ::open-socket url}))

(kf/reg-event-fx
  :input/on-key-down
  [(rf/inject-cofx ::getter/now)]
  (fn [{{:keys [session-id]} :db
        now                  :now}
       [value]]
    (rf/dispatch [::client>server
                  [{:user    [:session-id session-id]
                    :posted  now
                    :message value}]])
    nil))

(kf/reg-event-db
  ::timer
  (fn [db [now]]
    (assoc db
      :now now)))

(rf/reg-event-fx
  :nav/route-name
  (fn [_ [_ route-name]]
    {:navigate-to [route-name]}))

(kf/reg-event-db
  ::client>server
  (fn [{server :server-socket :as db}
       [datoms]]
    (if server
      (a/go (a/>! server [:web-apps.routes.websockets/client>server datoms]))
      (rf/dispatch [::disconnected true]))
    nil))

(kf/reg-event-db
  ::init-server>clients
  (fn [db [session-id]]
    (assoc db
      :session-id session-id)))

(rp/reg-event-fx
  ::server>clients
  (fn [ctx [_ db]]
    (db/sync-db db)
    nil))

(kf/reg-event-fx
  ::disconnected
  [(rf/inject-cofx ::getter/url)]
  (fn [{{server :server-socket
         :as    db} :db
        url         ::getter/url
        :as         ctx}
       [reconnect?]]
    (when server
      (when server (a/close! server))
      (merge
        {:db (assoc db :server-socket nil)}
        (when reconnect?
          {::open-socket url})))))

(defn- server>clients! [server]
  (a/go-loop []
    (if-let [db-event (:message (a/<! server))]
      (do
        (rf/dispatch
          db-event)
        (recur))
      (rf/dispatch [::disconnected false]))))

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
