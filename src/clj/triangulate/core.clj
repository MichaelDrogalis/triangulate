(ns triangulate.core
  (:require [compojure.core :refer [defroutes POST]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [clj-http.client :as client]
            [cheshire.core :refer [parse-string]]
            [triangulate.memcached])
  (:import [polyline PolylineDecoder]
           [polyline Location]))

(defn polyline [src dst]
  (let [query-params {:origin src :destination dst :sensor false}
        resp (client/get "http://maps.googleapis.com/maps/api/directions/json"
                         {:query-params query-params})]
    (-> resp :body (parse-string true) :routes first :overview_polyline :points)))

(defn decode-polyline [poly]
  (map (fn [x] {:lat (.latitude x) :long (.longitude x)})
       (PolylineDecoder/decodePoly poly)))

(defn haversine [a b]
  (let [lat1 (:lat a)
        lon1 (:long a)
        lat2 (:lat b)
        lon2 (:long b)
        radius-of-earth 6372800
        dlat (Math/toRadians (- lat2 lat1))
        dlon (Math/toRadians (- lon2 lon1))
        lat1 (Math/toRadians lat1)
        lat2 (Math/toRadians lat2)
        a (+ (* (Math/sin (/ dlat 2)) (Math/sin (/ dlat 2)))
             (* (Math/sin (/ dlon 2)) (Math/sin (/ dlon 2))
                (Math/cos lat1) (Math/cos lat2)))]
    (* radius-of-earth 2 (Math/asin (Math/sqrt a)))))

(defn interpolate-coordinates [a b whole piece]
  (let [lat1 (:lat a)
        lon1 (:long a)
        lat2 (:lat b)
        lon2 (:long b)
        lat-diff (- lat2 lat1)
        lon-diff (- lon2 lon1)
        theta (Math/atan (/ lon-diff lat-diff))
        x1 (* (Math/cos theta) piece)
        y1 (* (Math/sin theta) piece)
        x2 (* (Math/cos theta) whole)
        y2 (* (Math/sin theta) whole)
        x-ratio (* (/ x1 x2) lat-diff)
        y-ratio (* (/ y1 y2) lon-diff)]
    {:lat (+ lat1 x-ratio) :long (+ lon1 y-ratio)}))

(defn take-segments
  ([segments distance] (take-segments segments distance 0 0))
  ([[head & tail] distance k n]
     (if (>= k distance) n
         (recur tail distance (+ k head) (inc n)))))

(defn format-intx [intx extender]
  (str "Intersection of "
       (clojure.string/join " and " (:intersection/of intx))
       "," extender))

(defn target-coordinates [src dst gap extender]
  (let [formatted-src (format-intx src extender)
        formatted-dst (format-intx dst extender)
        coordinates (reverse (decode-polyline (polyline formatted-src formatted-dst)))
        pairs (partition 2 1 coordinates)
        segments (map (fn [[a b]] (haversine a b)) pairs)
        back-segment (take-segments segments gap)
        front-segment (dec back-segment)
        back-distance (reduce + (take back-segment segments)) 
        front-distance (reduce + (take front-segment segments))
        back-coordinate (nth coordinates back-segment)   
        front-coordinate (nth coordinates front-segment)
        front-to-back (- back-distance front-distance)
        front-to-target (- gap front-distance)]
    (interpolate-coordinates front-coordinate back-coordinate front-to-back front-to-target)))

(defroutes routes
  (POST "/rush-hour/api/triangulate/edn" {:keys [body]}
        (let [{:keys [src dst gap extender]} (read-string (slurp body))]
          (pr-str {:coordinates (target-coordinates src dst gap extender)}))))

(def app (wrap-params #'routes))

(defn -main [& args]
  (run-jetty #'app {:port 9092 :join? true}))

