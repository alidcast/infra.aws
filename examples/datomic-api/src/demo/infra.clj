(ns demo.infras
  (:require [rejure.infra.aws :as aws]))


(def cfg (aws/read-config "demo/aws-config.edn" :dev {}))
