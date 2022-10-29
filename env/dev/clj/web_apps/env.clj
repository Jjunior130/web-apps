(ns web-apps.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [web-apps.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[web-apps started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[web-apps has shut down successfully]=-"))
   :middleware wrap-dev})
