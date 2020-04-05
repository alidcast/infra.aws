(ns rejure.infra.aws
  "Utilities to manage AWS Cloudformation resources."
  (:require [clojure.data.json :as json]
            [cognitect.aws.client.api :as aws]))

(def ^:private clf-client 
  "Aws cloudformation client."
  (aws/client {:api :cloudformation}))

(defn- req-clf
  "Make aws cloudformation request."
  [op request]
  (aws/invoke clf-client {:op op
                          :request request}))

(defn- serialize-paramaters
  "Convert paramter map to AWS paramater array."
  [params]
  (into [] (for [[k v] params]
             {:ParameterKey (name k)
              :ParameterValue v})))

;; # Operations

(defn create-stack
  "Creates a stack, either as a template url or map of options.
   Template url expects vector, first arg is url string second is optional params map.
   Template map matches same options exposed by cloudformation."
  [name body]
  (if (vector? body)
    (let [[url params extra-opts] body]
      (req-clf :CreateStack (merge {:StackName name
                                    :TemplateURL url
                                    :Parameters (serialize-paramaters params)}
                               extra-opts)))
    (req-clf :CreateStack {:StackName name
                           :TemplateBody (json/write-str body)})))

(defn delete-stack [name]
  (req-clf :DeleteStack {:StackName name
                         :RetainResources []}))

