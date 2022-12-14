(ns web-apps.routes.home
  (:require
   [web-apps.layout :as layout]
   [clojure.java.io :as io]
   [web-apps.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [clojure.tools.logging :as l]))

(defn home-page [request]
  (layout/render (l/spy request) "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

