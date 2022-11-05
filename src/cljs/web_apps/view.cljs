(ns web-apps.view
  (:require
    [kee-frame.core :as kf]
    [markdown.core :refer [md->html]]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [re-posh.core :as rp]
    [web-apps.websockets :as ws]))

(defn nav-link [title page]
  [:a.navbar-item
   {:href  (kf/path-for [page])
    :class (when (= page @(rf/subscribe [:nav/page])) "is-active")}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "web-apps"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click    #(swap! expanded? not)
        :class       (when @expanded? :is-active)}
       [:span] [:span] [:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "Home" :home]
       [nav-link "About" :about]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(rp/reg-query-sub
  ::messages
  '[:find ?msg ?t
    :where
    [?e :message ?msg]
    [?e :posted ?t]])

(defn message-list []
  (fn []
    [:ul
     (for [[i [message t]] (take-last 10
                             (map-indexed vector
                               (sort-by second @(rf/subscribe
                                                  [::messages]))))]
       ^{:key i}
       [:li ((clojure.string/split (str t) " ") 4) " - " message])]))

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
           (when-let [v @value]
             (rf/dispatch [::ws/client>server
                           [{:message v
                             :posted  (js/Date.)}]]))
           (reset! value nil))}])))

(defn home-page []
  [:section.section>div.container>div.content
   [:div.container
    [:div.row
     [:div.col-md-12
      [:h2 "Welcome to chat"]]]
    [:div.row
     [:div.col-sm-6
      [message-list]]]
    [:div.row
     [:div.col-sm-6
      [message-input]]]]
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn root-component []
  [:div
   [navbar]
   [kf/switch-route (fn [route] (get-in route [:data :name]))
    :home home-page
    :about about-page
    nil [:div ""]]])
