(ns web-apps.db
  (:require
    [datascript.core :as d]
    [re-posh.core :as rp]
    [re-frame.core :as rf]))

(def conn (datascript.core/create-conn))

(re-posh.core/connect! conn)

(defn sync-db [server-db]
  (d/reset-conn! conn server-db))