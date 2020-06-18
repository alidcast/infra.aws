(ns datomic-demo.infra
  (:require [infra.aws.config :as ic]
            [clojure.java.io :as io]))

(def cfg (ic/read-edn (slurp (io/reader (io/resource "datomic_demo/aws-stacks.edn"))) :dev))
