(ns web-apps.datsync)

(defn- server>clients
  "The LinkedBlockingQueue method .take will block the thread until something is put into the tx-report-queue.
  ((protocols/commit fn1) return-value) is needed for this function to work as <! inside an async go block"
  [tx-report-queue fn1]
  (let [tx-report (.take tx-report-queue)]
    (->> tx-report
         (vector :web-apps.websockets/server>clients)
         ((clojure.core.async.impl.protocols/commit fn1)))))

(extend java.util.concurrent.LinkedBlockingQueue
  clojure.core.async.impl.protocols/ReadPort
  {:take! server>clients})