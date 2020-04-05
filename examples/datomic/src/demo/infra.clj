(ns demo.infra
  (:require [rejure.infra.core :as infra]))

(def cfg (infra/read-config "demo/aws-config.edn" :dev {}))
