(ns web-apps.db.core
  (:require
    [datascript.core :as d]
    [clojure.core.async :as a]
    #_[io.rkn.conformity :as c]
    [mount.core :refer [defstate]]))

(def schema
  {:message {:db/cardinality :db.cardinality/one}
   :posted {:db/cardinality :db.cardinality/one}})

(defstate conn
  :start (d/create-conn schema)
  :stop nil)

(defn db []
  (d/db conn))

(defn sync-db [client]
  (a/go
    (a/>! client [:web-apps.websockets/server>clients (db)])))

(defn transact [tx]
  (d/transact conn tx))

(defn datoms [index & args]
  (apply d/datoms (d/db conn) index args))

#_(defn install-schema
    "This function expected to be called at system start up.

  Datomic schema migrations or db preinstalled data can be put into 'migrations/schema.edn'
  Every txes will be executed exactly once no matter how many times system restart."
    [conn]
    (let [norms-map (c/read-resource "migrations/schema.edn")]
      (c/ensure-conforms conn norms-map (keys norms-map))))

(defn show-schema
  "Show currently installed schema"
  [conn]
  (let [system-ns #{"db" "db.type" "db.install" "db.part"
                    "db.lang" "fressian" "db.unique" "db.excise"
                    "db.cardinality" "db.fn" "db.sys" "db.bootstrap"
                    "db.alter"}]
    (d/q '[:find ?ident
           :in $ ?system-ns
           :where
           [?e :db/ident ?ident]
           [(namespace ?ident) ?ns]
           [((comp not contains?) ?system-ns ?ns)]]
      (d/db conn) system-ns)))

#_(defn show-transaction
    "Show all the transaction data
   e.g.
    (-> conn show-transaction count)
    => the number of transaction"
    [conn]
    (seq (d/tx-range (d/log conn) nil nil)))

(defn add-user
  "e.g.
    (add-user conn {:id \"aaa\"
                    :screen-name \"AAA\"
                    :status :user.status/active
                    :email \"aaa@example.com\" })"
  [conn {:keys [id screen-name status email]}]
  @(d/transact conn [{:user/id     id
                      :user/name   screen-name
                      :user/status status
                      :user/email  email}]))

(defn find-one-by
  "Given db value and an (attr/val), return the user as EntityMap (datomic.query.EntityMap)
   If there is no result, return nil.

   e.g.
    (d/touch (find-one-by (d/db conn) :user/email \"user@example.com\"))
    => show all fields
    (:user/first-name (find-one-by (d/db conn) :user/email \"user@example.com\"))
    => show first-name field"
  [db attr val]
  (d/entity db
    ;;find Specifications using ':find ?a .' will return single scalar
    (d/q '[:find ?e .
           :in $ ?attr ?val
           :where [?e ?attr ?val]]
      db attr val)))


(defn find-user [db id]
  (d/touch (find-one-by db :user/id id)))

(comment
  (install-schema conn)
  (show-schema conn)
  (show-transaction conn))