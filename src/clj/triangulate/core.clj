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
                                {:query-params {:origin "Intersection of Main Street and Stephenson Street, Duryea, PA"
                                                :destination "Intersection of Main Street and Phoenix Street, Duryea, PA"
                                                :sensor false}}))]
    (-> (parse-string body true) :routes first :overview_polyline :points)))

(defn decode-polyline [poly]
  (map (fn [x] {:lat (.latitude x) :long (.longitude x)}) (PolylineDecoder/decodePoly poly)))

(defn haversine [a b]
  (let [lat1 (:lat a)
        lon1 (:long a)
        lat2 (:lat b)
        lon2 (:long b)
        R 6372800
        dlat (Math/toRadians (- lat2 lat1))
        dlon (Math/toRadians (- lon2 lon1))
        lat1 (Math/toRadians lat1)
        lat2 (Math/toRadians lat2)
        a (+ (* (Math/sin (/ dlat 2)) (Math/sin (/ dlat 2)))
             (* (Math/sin (/ dlon 2)) (Math/sin (/ dlon 2))
                (Math/cos lat1) (Math/cos lat2)))]
    (* R 2 (Math/asin (Math/sqrt a)))))

(defn interpolate-coordinates [a b c d]
  (let [lat1 (:lat a)
        lon1 (:long a)
        lat2 (:lat b)
        lon2 (:long b)
        lat-diff (- lat2 lat1)
        lon-diff (- lon2 lon1)
        theta (Math/atan (/ lon-diff lat-diff))
        x1 (* (Math/cos theta) d)
        y1 (* (Math/sin theta) d)
        x2 (* (Math/cos theta) c)
        y2 (* (Math/sin theta) c)
        x-ratio (* (/ x1 x2) lat-diff)
        y-ratio (* (/ y1 y2) lon-diff)]
    {:lat (+ lat1 x-ratio) :long (+ lon1 y-ratio)}))

(defn take-segments
  ([segments distance] (take-segments segments distance 0 0))
  ([[head & tail] distance k n]
     (if (>= k distance)
       n
       (recur tail distance (+ k head) (inc n)))))

(def coordinates (reverse (decode-polyline (polyline))))

(def meters-away 200)

(def pairs (partition 2 1 coordinates))

(def segments (map (fn [[a b]] (haversine a b)) pairs))

(def back-segment (take-segments segments meters-away))

(def front-segment (dec back-segment))

(def back-distance (reduce + (take back-segment segments)))

(def front-distance (reduce + (take front-segment segments)))

(def back-coordinate (nth coordinates back-segment))

(def front-coordinate (nth coordinates front-segment))

(def distance-to-front-segment (- back-distance meters-away))

(def target-to-front-segment (- meters-away front-distance))

;;(pprint coordinates)


(defroutes routes
  (POST "/rush-hour/api/triangulate/edn" {:keys [body]}
        (let [{:keys [src dst]} (read-string (slurp body))]
          (pr-str {}))))

(def app (wrap-params #'routes))

(defonce jetty (run-jetty #'app {:port 9092 :join? false}))

