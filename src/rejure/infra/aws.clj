(ns rejure.infra.aws
  "Utilities to manage AWS Cloudformation resources."
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [cognitect.aws.client.api :as aws]
            [aero.core :as aero :refer [reader]]))

;; # Config Reader

(defn url-template? [v]
  (vector? v))

(defn- serialize-params
  "Convert paramter map to AWS paramater array."
  [params]
  (into [] (for [[k v] params]
             {:ParameterKey (name k)
              :ParameterValue v})))

(defn serialize-opts
  [template env]
  (reduce-kv
   (fn [m k v]
     (let [stack-name (str (name k) "-" (name env))
           template   (if (url-template? v)
                          (let [[url params extra-opts] v]
                            (merge {:StackName stack-name
                                    :TemplateURL url
                                    :Parameters (serialize-params params)}
                                   extra-opts))
                          {:StackName stack-name
                           :TemplateBody v
                           ;; (json/write-str v)
                           })]
       (assoc m stack-name template)))
   {}
   template))

;; -- Custom `reader` tags \start
(defmethod reader 'param
  ;; Replace param key with value.
  ;; i.e. #param :foo -> "bar"
  [{:keys [params]} _ k]
  (get params k))

(defmethod reader 'format
  ;; Helper for formatting strings, using Clojure's `format` function.
  ;; i.e. #format ["%s-%s" "foo" "bar"] ->  "foo-bar"
  [_ _ [fmt & args]]
  (apply format fmt args))

(defmethod reader 'stack
  [_ _ k]
  (str (name k) "-dev"))
;; -- Custom `reader` tags \end

(defn read-config
  "Read an AWS resource configuration.
   Takes a resource path `path`, env keyword `env` and parameter map `params` as arguments.
   Expects edn config to be mapping of resource names to template options.
   Returns that same mapping of resources with their template options serialized to match aws spec.
   See `serialize-opts` for templating details."
  ([path env] (read-config path env {}))
  ([path env params]
   (serialize-opts (aero/read-config (io/resource path) {:profile env
                                                         :params  params})
                       env)))

;; # Cloudformation Operations

(def ^:private clf-client 
  "Aws cloudformation client."
  (aws/client {:api :cloudformation}))

(defn- req-clf
  "Make aws cloudformation request."
  [op request]
  (aws/invoke clf-client {:op op
                          :request request}))

(defn create-stack
  "Creates a stack, either as a template url or map of options."
  [opts]
  (req-clf :CreateStack opts))

(defn delete-stack [name]
  (req-clf :DeleteStack {:StackName name
                         :RetainResources []}))
