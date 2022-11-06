(ns web-apps.getter
  (:require
    [re-frame.core :as rf]
    [re-posh.core :as rp]))

(rf/reg-cofx
  ::now
  (fn [cofx]
    (assoc cofx
      :now (js/Date.))))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(rp/reg-query-sub
  ::messages
  '[:find ?msg ?t ?username
    :where
    [?e :user ?u]
    [?u :username ?username]
    [?e :message ?msg]
    [?e :posted ?t]])

(rp/reg-query-sub
  ::username
  '[:find ?username .
    :in $ ?s-id
    :where
    [?u :session-id ?s-id]
    [?u :username ?username]])

(rf/reg-sub
  ::now
  #(:now %))

(defn h-mm-ss [date]
  (let [h (.getHours date)
        m (.getMinutes date)
        s (.getSeconds date)
        a (if (> h 12) "pm" "am")
        h (mod h 12)
        m (if (>= m 10) m (str "0"m))
        s (if (>= s 10) s (str "0"s))]
    (str h":"m":"s" "a)))

(rf/reg-sub
  ::now-h:mm:ss
  :<- [::now]
  h-mm-ss)

(rf/reg-sub
  :nav/route
  :<- [:kee-frame/route]
  identity)

(rf/reg-sub
  :nav/page
  :<- [:nav/route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  ::session-id
  #(:session-id %))

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
