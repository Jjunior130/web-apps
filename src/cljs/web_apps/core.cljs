(ns web-apps.core
  (:require
    [chord.client :as ws]
    [kee-frame.websocket :as websocket]
    [kee-frame.core :as kf]
    [re-frame.core :as rf]
    [ajax.core :as http]
    [web-apps.ajax :as ajax]
    [web-apps.routing :as routing]
    [web-apps.view :as view]))

(rf/reg-event-fx ::start-socket
                 (fn [_ _]
                   {::websocket/open {:path      "/"
                                      :dispatch  ::on-event-receive}}))

(kf/reg-controller
  ::socket-controller
  {:params (constantly true)
   :start  [::start-socket]})

(rf/reg-event-fx
  ::load-about-page
  (constantly nil))

(kf/reg-controller
  ::about-controller
  {:params (constantly true)
   :start  [::load-about-page]})

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(kf/reg-chain
  ::load-home-page
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/docs"
                  :response-format (http/raw-response-format)
                  :on-failure      [:common/set-error]}})
  (fn [{:keys [db]} [_ docs]]
    {:db (assoc db :docs docs)}))


(kf/reg-controller
  ::home-controller
  {:params (constantly true)
   :start  [::load-home-page]})

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components
  []
  (rf/clear-subscription-cache!)
  (kf/start! {:routes         routing/routes
              :hash-routing?  true
              #_#_
              :log            {:level        :debug
                               :ns-blacklist ["kee-frame.event-logger"]}
              :initial-db     {}
              :root-component [view/root-component]}))

(defn init! []
  (ajax/load-interceptors!)
  (mount-components))
