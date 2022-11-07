(ns web-apps.chat
  (:require
    [re-frame.core :as rf]
    [kee-frame.core :as kf]
    [re-posh.core :as rp]
    [web-apps.getter :as getter]
    [web-apps.setter :as setter]))

(defn message-list []
  [re-com.core/v-box
   :children
   (for [[i [message t username]]
         (take-last 10
           (map-indexed vector
             (sort-by second @(rf/subscribe
                                [::getter/messages]))))]
     ^{:key i}
     [re-com.core/h-box
      :children
      [(getter/h-mm-ss t)
       " - "
       username ": "
       message]])])

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
       {:type        :text
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
    #(let [now (js/Date.)]
       (rf/dispatch [::setter/timer now]))
    1000))

(defn chat-page []
  [:section.section>div.container>div.content
   [re-com.core/v-box
    :children [[:h2 "Welcome to chat"]
               [message-list]
               [re-com.core/h-box
                :children [@(rf/subscribe [::getter/now-h:mm:ss])
                           " - "
                           @(rf/subscribe [::getter/username @(rf/subscribe [::getter/session-id])])
                           ": "
                           [message-input]]]]]])

