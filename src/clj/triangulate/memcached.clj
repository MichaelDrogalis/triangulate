(ns triangulate.memcached
  (:require [clojurewerkz.spyglass.client :as c]))

(def twenty-four-hours (* 60 60 24))

(def conn (c/text-connection "127.0.0.1:11211"))

(defn serialize-coordinates [src dst]
  (pr-str {:src src :dst dst}))

(defn cache-polyline [src dst polyline]
  (c/set conn (serialize-coordinates src dst) twenty-four-hours polyline))

(defn cached-polyline [src dst]
  (c/get conn (serialize-coordinates src dst)))

