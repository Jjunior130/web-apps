(ns web-apps.env
  (:require
    [selmer.parser :as parser]
    [web-apps.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!))
   :stop
   (fn [])
   :middleware wrap-dev})
