(ns web-apps.chat
  (:require
    [re-frame.core :as rf]
    [kee-frame.core :as kf]
    [re-posh.core :as rp]
    [web-apps.getter :as getter]
    [web-apps.setter :as setter]))

(defn message-list []
  (fn []
    [:div {:style {:display "flex"
                   :flex-direction "column"
                   :flex "1"
                   :justify-content "space-between"}}
     (for [[i [message t username]]
           (take-last 25
             (map-indexed vector
               (sort-by second @(rf/subscribe
                                  [::getter/messages]))))]
       ^{:key i}
       [:div
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
       {:style {:flex "1"}
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
  [:div.content.container
   {:style {:margin-top      "0.2rem"
            :margin-left     "0.8rem"
            :display         "flex"
            :flex-direction  "column"
            :height          "100%"}}
   [:h2 {:style {:flex "0"}} "Welcome to chat"]
   [message-list]
   [:div {:style {:display "flex"
                  :align-items "center"
                  :flex "0"}}
    @(rf/subscribe [::getter/now-h:mm:ss])
    " - "
    @(rf/subscribe [::getter/username @(rf/subscribe [::getter/session-id])])
    ": "
    [message-input]]])
