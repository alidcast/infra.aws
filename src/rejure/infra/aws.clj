(ns rejure.infra.aws
  "Utilities to manage AWS Cloudformation resources."
  (:require [cognitect.aws.client.api :as aws]))

;; # Cloudformation operations

(def ^:private clf-client 
  "Aws cloudformation client."
  (aws/client {:api :cloudformation}))

(defn- req-clf
  "Make an aws cloudformation request."
  [op request]
  (aws/invoke clf-client {:op op
                          :request request}))

(defn create-stack
  [opts]
  (req-clf :CreateStack opts))

(defn describe-stack 
  [opts]
  (req-clf :DescribeStack opts))

(defn delete-stack [name]
  (req-clf :DeleteStack {:StackName name
                         :RetainResources []}))
