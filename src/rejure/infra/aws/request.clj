(ns rejure.infra.aws.request
  "Send AWS Cloudformation requests."
  (:require [cognitect.aws.client.api :as aws]))

;; # Cloudformation Operations 

(def ^:private clf-client 
  "Cloudformation client."
  (aws/client {:api :cloudformation}))

(defn- req-clf
  "Make an Cloudformation request."
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
