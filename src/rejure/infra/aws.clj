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
             {:ParameterKey   (name k)
              :ParameterValue v})))

(defn serialize-config
  "Expects mapping of resource names to template options.
   A template options can either be a vector, if template is dervied from a url, or a map.
   For vector first arg is url string, second is parameter map, and third is extra aws options
   The key-value parameters are serialized to match aws spec and merged with the extra aws options."
  [template]
  (reduce-kv
   (fn [m k v]
     (let [template   (if (url-template? v)
                        (let [[url params extra-opts] v]
                          (merge {:StackName   k
                                  :TemplateURL url
                                  :Parameters  (serialize-params params)}
                                 extra-opts))
                        {:StackName    k
                         :TemplateBody v
                           ;; (json/write-str v)
                         })]
       (assoc m k template)))
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

(defmethod reader 'eid
  ;; Append environment info to identifier key.
  ;; i.e. #eid :foo -> "foo-dev"
  [{:keys [profile]} _ k]
  (str (name k) "-" (name profile)))
;; -- Custom `reader` tags \end

(defn read-config
  "Read an AWS resource configuration.
   Takes a resource path `path`, env keyword `env` and parameter map `params` as arguments.
   Returns mapping of resources with their template options serialized to match aws spec.
   See `serialize-config` for templating details."
  ([path env] (read-config path env {}))
  ([path env params]
   (serialize-config (aero/read-config (io/resource path) {:profile env
                                                           :params  params}))))

;; # Cloudformation Operations

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

(defn delete-stack [name]
  (req-clf :DeleteStack {:StackName name
                         :RetainResources []}))
