(ns triangulate.memcached
  (:require [clojurewerkz.spyglass.client :as c]))

(def twenty-four-hours (* 60 60 24))

(def key-mapping (atom {}))

(def conn (c/text-connection "rush-hour.bsfnjw.cfg.usw2.cache.amazonaws.com:11211"))

(defn serialize-coordinates [src dst]
  (let [pair {:src src :dst dst}]
    (str (or (get @key-mapping pair)
             (let [mkey (java.util.UUID/randomUUID)]
               (swap! key-mapping assoc pair mkey)
               mkey)))))

(defn cache-polyline [src dst polyline]
  (c/set conn (serialize-coordinates src dst) twenty-four-hours polyline))

(defn cached-polyline [src dst]
  (c/get conn (serialize-coordinates src dst)))

