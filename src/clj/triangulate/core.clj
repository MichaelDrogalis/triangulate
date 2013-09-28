(ns triangulate.core
  (:require [compojure.core :refer [defroutes POST]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [clj-http.client :as client]
            [cheshire.core :refer [parse-string]]
            [clojure.pprint :refer [pprint]])
  (:import [polyline PolylineDecoder]
           [polyline Location]))

(defn polyline []
  (let [body (:body (client/get "http://maps.googleapis.com/maps/api/directions/json"
                                {:query-params {:origin "240 East Morton Street, Old Forge"
                                                :destination "245 East Morton Street, Old Forge"
                                                :sensor false}}))]
    (-> (parse-string body true) :routes first :overview_polyline :points)))

(defn decode-polyline [poly]
  (map (fn [x] {:lat (.latitude x) :long (.longitude x)}) (PolylineDecoder/decodePoly poly)))

(pprint (decode-polyline (polyline)))

(defroutes routes
  (POST "/rush-hour/api/triangulate/edn" {:keys [body]}
        (let [{:keys [src dst]} (read-string (slurp body))]
          (pr-str {}))))

(def app (wrap-params #'routes))

(defonce jetty (run-jetty #'app {:port 9092 :join? false}))

