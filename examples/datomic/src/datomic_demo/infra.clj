(ns datomic-demo.infra
  (:require [rejure.infra.aws.config :as ic]
            [clojure.java.io :as io]))

(def cfg (ic/read-edn (io/reader "resources/demo/datomic-stacks.edn") :dev {}))
