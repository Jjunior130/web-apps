(ns web-apps.db
  (:require
    [datascript.core :as d]
    [re-posh.core :as rp]))

(def conn (datascript.core/create-conn))

(re-posh.core/connect! conn)