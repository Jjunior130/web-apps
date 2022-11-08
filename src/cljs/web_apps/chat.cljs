(ns web-apps.chat
  (:require
    [re-frame.core :as rf]
    [kee-frame.core :as kf]
    [re-posh.core :as rp]
    [web-apps.getter :as getter]
    [web-apps.setter :as setter]))

(defn message-list []
  (fn []
    [:div {:style {:overflow :auto
                   :display "flex"
                   :flex-direction "column-reverse"
                   :justify-content "space-between"}}
     (for [[i [message t username]]
           (->> @(rf/subscribe
                   [::getter/messages])
             (sort-by second)
             (map-indexed vector)
             (take-last 30)
             reverse)]
       ^{:key i}
       [:div {:display "flex"}
        (getter/h-mm-ss t)
        " - "
        username
        ": "
        message])]))

(defn message-input
  "type in a message and send it to the server.
  This component creates a local atom to keep
  track of the message being typed in and sends
  the message to the server when the enter key
  is pressed."
  []
  (let [value (reagent.core/atom nil)]
    (fn []
      [:input.form-control
       {:style       {:flex "auto"}
        :type        :text
        :placeholder "type in a message and press enter"
        :value       @value
        :on-change
        #(reset! value (-> % .-target .-value))
        :on-key-down
        #(when (= (.-keyCode %) 13)
           (when (not-every? (fn [c] (= c " ")) @value)
             (rf/dispatch [:input/on-key-down @value]))
           (reset! value nil))}])))

(defonce now
  (js/setInterval
    #(rf/dispatch [::setter/timer (js/Date.)])
    1000))

(defn chat-page []
  [:div.content
   {:style {:overflow :auto
            :margin-top     "0.2rem"
            :margin-left    "0.8rem"
            :display        "flex"
            :flex-direction "column"}}
   [:h2 {:style {:margin-bottom "auto"}}
    "Welcome to chat"]
   [message-list]
   [:div {:style {:display     "flex"
                  :align-items "center"}}
    [:div
     @(rf/subscribe [::getter/now-h:mm:ss])
     " - "
     @(rf/subscribe [::getter/username @(rf/subscribe [::getter/session-id])])
     ": "]
    [message-input]]])
