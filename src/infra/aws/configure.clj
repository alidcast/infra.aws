(ns infra.aws.configure "Configure AWS Cloudformation templates."
    (:require [clojure.edn :as edn]
              [clojure.string :as string]))

;; -- Config Helpers --
;; Exposed helpers for retrieving serialized config info.

(defn eid "Creates environment unique id based on identifier key `k` and `env` keyword."
  [k env]
  (str (name k) "-" (name env)))

(defn get-ssm-param-keys "Gets all System Manager Parameter keys declared in config's `cfg` templates."
  [cfg]
  (reduce (fn [acc [_ tplate]]
            (if (map? (:TemplateBody tplate))
              (let [ks (reduce (fn [acc [_ v]]
                                 (if (= (:Type v) "AWS::SSM::Parameter")
                                   (conj acc (:Name (:Properties v)))
                                   acc))
                               []
                               (:Resources (:TemplateBody tplate)))]
                (into acc ks))
              acc))
          []
          cfg))

;; -- Config Serialization Factorty --
;; Allows shorthand declarations for AWS Cloudformation templates. See [serialize-config] for details.

(defn- url-tplate? "Checks whether value `v` is a url template declaration."
  [v]
  (vector? v))

(defn- k->aws-resource-type
  "Covert keyword `k` to a AWS physical resource identifier type.
   e.g. :Aws.Service/Module-> AWS::Service::Module"
  [k]
  (let [ns-str (namespace k)]
    (if ns-str
      (string/join "::" (conj (string/split ns-str #"\.") (name k)))
      (ex-info "AWS resource type must be in format :AWS.Service/Module." {:key key}))))

(defn- make-aws-url-tplate "Makes AWS template from stack `name` and template `url`."
  [name url]
  {:StackName name :TemplateURL url})

(defn- make-aws-opts-tplate
  "Makes AWS template from stack `name` and options `opts`.
   Allows declaring resources as type+properties tuples that are converted into their map form."
  [name opts]
  {:StackName    name
   :TemplateBody (assoc opts
                        :Resources
                        (reduce-kv (fn [m k v]
                                     (assoc m  k (if (vector? v)
                                                   {:Type (k->aws-resource-type (first v))
                                                    :Properties (second v)}
                                                   v)))
                                   {}
                                   (:Resources opts)))})


(defn serialize-config
  "Expects config `cfg` to be a mapping of resource names to template options.
   A template options can either be a vector, if template is dervied from a url, or a map.
   For vector, first arg is url string and second is aws options.
   For map, can declare AWS options directly but we provide a tuple shorthand for declararing 
   a resources, see `->aws-tplate-body` for details."
  [cfg]
  (let [clf (:cloudformation/stacks cfg)]
    (reduce-kv
     (fn [m k v]
       (let [template   (if (url-tplate? v)
                          (let [[url clf-opts] v]
                            (merge (make-aws-url-tplate k url)
                                   clf-opts))
                          (make-aws-opts-tplate k v))]
         (assoc m k template)))
     {}
     clf)))

;; -- Config Reader Literals Factory

(defn- kv-params->aws-params "Converts key-value map `m` to AWS parameter array specfication."
  [m]
  (into [] (for [[k v] m] {:ParameterKey (name k) :ParameterValue v})))

(defn- k->aws-ref "Coverts key `k` to AWS logical resource reference."
  [k] {"Ref" (name k)})

(defn k->aws-import-value "Convert key `k` to AWS import value stack reference function."
  [k] {"Fn::ImportValue" k})

(defn- with-aws-ssm-params
  "Auto-includes a Sytem Manager Parameter for each declared resource identifier in map `m`."
  [m]
  (reduce-kv
   (fn [acc k _]
     (assoc acc
            (keyword (str (name k) "Param"))
            {:Type "AWS::SSM::Parameter"
             :Properties {:Name (name k) :Type "String" :Value (k->aws-ref k)}}))
   m
   m))

(defn create-readers
  "Create edn reader literals using `env` keyword and `param` map.
   Where appropriate we match the AWS function utilities provided for YAML/JSON templates."
  [env params]
  {'eid (fn [k] (eid k env))
   'aws/params kv-params->aws-params
   'aws/with-ssm-params with-aws-ssm-params
   ;; aws built-ins
   'aws/ref k->aws-ref
   'aws/import k->aws-import-value
   'aws/sub (fn [k] (get params k))})

(def default-opts {:params {}
                   :extra-readers {}})
(defn read-edn
  "Reads config edn string `s` based on environment keyword `env` and an optional `params` map.
   See `serialize-config` for templating details."
  ([s env] (read-edn s env default-opts))
  ([s env opts]
   (let [{:keys [params extra-readers]} (merge default-opts opts)]
     (serialize-config
      (edn/read-string {:readers (merge (create-readers env params) extra-readers)}
                       s)))))
