(ns demo.aws-cognito-infra
  (:require [rejure.infra.aws.config :as infra-cfg]
            [demo.utils.fs :as fs]))

(def cfg (infra-cfg/read-edn (fs/resource "aws-cognito-config.edn") :dev {}))
