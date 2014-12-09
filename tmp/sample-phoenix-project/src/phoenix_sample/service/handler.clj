(ns phoenix-sample.service.handler
  (:require [phoenix-sample.service.server :as s]
            [phoenix-sample.service.db :as db]
            [ring.util.response :refer [response content-type]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [bidi.ring :refer [make-handler]]
            [com.stuartsierra.component :refer [Lifecycle]]))

(def routes
  [["/object/" :oid] :object-lookup #_{:get :object-lookup
                                  :put :object-set}])

(bidi.bidi/match-route routes "/object/231")


(defn handlers [db]
  {:lookup (fn [req]
             (get @db (get-in req [:route-params :oid])))

   :object-set (fn [req]
                 (swap! db
                        assoc
                        (get-in req [:route-params :oid])
                        (:body-params req)))})

(defrecord AppHandler [opts]
  Lifecycle
  (start [{:keys [db] :as this}]
    (println "starting handler with db:" (pr-str db))

    (db/put-obj! db :foo :bar)
    (println "Foo is:" (db/get-obj db :foo))
    
    this)
  (stop [this] this)

  s/WebHandler
  (make-handler [{:keys [db] :as opts}]
    (-> (make-handler routes (handlers db))
        (wrap-restful-format :formats [:edn]))))
