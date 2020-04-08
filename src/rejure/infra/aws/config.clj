(ns rejure.infra.aws.config
  "Configure AWS Cloudformation templates with EDN."
  (:require [clojure.edn :as edn]
            [clojure.string :as string]))

;; # Config Helpers

(defn eid 
  "Returns identifier key based on environment.
   Takes `ident` keyword and `env` keyword as arguments."
  [ident env]
  (str (name ident) "-" (name env)))

;; # Config Serializer

(defn- template-url? [v]
  (vector? v))

(defn- ->aws-template-url
  "Accepts stack name keyword `k` and template `url` string."
  [k url]
  {:StackName   k
   :TemplateURL url})

(defn- ->aws-resource-type
  "Converts resource key `k` to AWS identifier type.
   The key should be in ':<Service>.<Module>' format.
   i.e. :Service.Module -> AWS::Service::Module"
  [k]
  (string/join "::" (cons "AWS" (string/split (name k) #"\."))))

(defn- ->aws-template-body
  "Accepts stack name keyword `k` and template map `body`.
   Expands a resource's tuple shorthand into AWS type+properties map."
  [k body]
  {:StackName    k
   :TemplateBody (assoc body 
                        :Resources 
                        (reduce-kv (fn [m k v]
                                     (assoc m  k (if (vector? v)
                                                     {:Type (->aws-resource-type (first v))
                                                      :Properties (second v)}
                                                     v)))
                                   {}
                                   (:Resources body)))})


;; TODO: guard against invalid vector shorthand count 
;; TODO: guard against invalid resource map
(defn serialize-config
  "Expects mapping of resource names to template options.
   A template options can either be a vector, if template is dervied from a url, or a map.
   For vector, first arg is url string and second is aws options.
   For map, can declare aws options directly, but we provide shorthand for `:Resource` property."
  [config]
  (reduce-kv
   (fn [m k v]
     (let [template   (if (template-url? v)
                        (let [[url clf-opts] v]
                          (merge (->aws-template-url k url)
                                 clf-opts))
                        (->aws-template-body k v))]
       (assoc m k template)))
   {}
   config))

;; # Config Reader Literals

(defn- kv-params->aws-params
  "Convert param map `m` to AWS paramater array."
  [m]
  (into [] (for [[k v] m] {:ParameterKey (name k) :ParameterValue v})))

(defn- k->aws-resource-ref 
  "Convert key `k` to AWS logical resource reference declaration."
  [k]
  {:Ref (name k)})

(defn- with-aws-ssm-param-resources
  "Include a sytem manager parameter resource for each key in resource map."
  [m]
  (reduce-kv
   (fn [acc k _]
     (assoc acc
            (keyword (str (name k) "Param"))
            [:SSM.Parameter {:Name (name k) :Value (k->aws-resource-ref k) :Type "String"}]))
   m
   m))

(defn create-readers 
  "Create edn reader literals.
   Where appropriate we match the AWS function utilities provided for YAML/JSON templates.
   (ref: https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference.html)"
  [env params]
  {'eid (fn [k] (eid k env))
   'kvp kv-params->aws-params
   'ref k->aws-resource-ref
   'sub (fn [k] (get params k))
   'with-ssm-params with-aws-ssm-param-resources})

;; # Config Reader 

;; TODO prefix resources with clf 
(defn read-edn
  "Read an AWS edn configuration.
   Takes an edn string `s`, env keyword `env` and parameter map `params` as arguments.
   Returns mapping of resources with their template options serialized to match aws spec.
   See `serialize-config` for templating details."
  ([s env] (read-edn s env {}))
  ([s env params]
   (serialize-config (edn/read-string {:readers (create-readers env params)}
                                      s))))
