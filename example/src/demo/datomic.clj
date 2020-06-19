(ns demo.datomic
  (:require [infra.aws.configure :as ic]
            [clojure.java.io :as io]))

(def aws-cfg "Datomic AWS Cloudformation config."
  (ic/read-edn (slurp (io/reader (io/resource "demo/datomic.edn"))) :dev))
