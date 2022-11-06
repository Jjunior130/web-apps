(ns web-apps.core
  (:require
    [kee-frame.core :as kf]
    [re-frame.core :as rf]
    [web-apps.ajax :as ajax]
    [web-apps.routing :as routing]
    [web-apps.view :as view]
    [web-apps.db]
    [web-apps.setter :as setter]))

(kf/reg-controller
  ::about-controller
  {:params (constantly true)
   :start  [::setter/load-about-page]})

(kf/reg-controller
  ::home-controller
  {:params (constantly true)
   :start  [::setter/load-home-page]})

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components
  []
  (rf/clear-subscription-cache!)
  (kf/start! {:routes         routing/routes
              :hash-routing?  true
              #_#_:log {:level        :debug
                        :ns-blacklist ["kee-frame.event-logger"]}
              :root-component [view/root-component]}))

(defn init! []
  (ajax/load-interceptors!)
  (rf/dispatch [::setter/init-client])
  (mount-components))
