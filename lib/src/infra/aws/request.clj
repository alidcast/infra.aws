(ns infra.aws.request "Request AWS Cloudformation operations."
  (:require [clojure.data.json :as json]
            [cognitect.aws.client.api :as aws]))

;; # Cloudformation Operations

(def clf-client "Clouformation client."
  (aws/client {:api :cloudformation}))

(defn- req-clf "Request Cloudformation operation `op` with options `opts`."
  [op opts]
  (aws/invoke clf-client {:op op
                          :request opts}))

(defn- serialize-opts 
  "If template body is present parse it to JSON; leave other options as is."
  [opts]
  (if (:TemplateBody opts)
    (assoc opts
           :TemplateBody 
           (json/write-str (:TemplateBody opts)))
    opts))

(defn create-stack
  [opts]
  (req-clf :CreateStack (serialize-opts opts)))

(defn describe-stack
  [opts]
  (req-clf :DescribeStack opts))

;; TODO add guard for deleting production stacks
(defn delete-stack [name]
  (req-clf :DeleteStack {:StackName name
                         :RetainResources []}))

