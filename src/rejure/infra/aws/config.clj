(ns rejure.infra.aws.config
  "Configure AWS Cloudformation templates with EDN."
  (:require [clojure.edn :as edn]
            [clojure.string :as string]))

;; # Config Helpers
;; Useful for retrieving serialized config info.
  
(defn eid 
  "Returns environment unique id based on identifier key `k` and `env` keyword."
  [k env]
  (str (name k) "-" (name env)))

(defn get-ssm-param-keys
  "Returns all System Manager Parameter keys declared in config map `cfg` templates."
  [cfg]
  (reduce (fn [acc [_ template]]
            (if (map? template)
              (let [ks (for [[k v] template]
                         (when (= (:Type v) "AWS::SSM::Parameter")
                           k))]
                (into acc ks))
              acc))
          []
          cfg))

;; # Config Serializer

(defn- template-url? [v]
  (vector? v))

(defn- ->aws-resource-type
  "Return AWS physical resource identifier type based on resource key `k`.
   The key should be in ':<Service>.<Module>' format, i.e, :Service.Module -> AWS::Service::Module"
  [k]
  (string/join "::" (cons "AWS" (string/split (name k) #"\."))))

(defn- ->aws-template-url
  "Returns AWS template url options based on stack `name` and `url`."
  [name url]
  {:StackName name :TemplateURL url})

(defn- ->aws-template-body
  "Returns AWS template body options based on stack `name` and template `body`.
   Expands a resource's tuple shorthand into AWS type+properties map."
  [name body]
  {:StackName    name
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
  "Expects config `cfg` to be a mapping of resource names to template options.
   A template options can either be a vector, if template is dervied from a url, or a map.
   For vector, first arg is url string and second is aws options.
   For map, can declare AWS options directly but we provide a tuple shorthand for declararing 
   a resources, see `->aws-template-body` for details."
  [cfg]
  (reduce-kv
   (fn [m k v]
     (let [template   (if (template-url? v)
                        (let [[url clf-opts] v]
                          (merge (->aws-template-url k url)
                                 clf-opts))
                        (->aws-template-body k v))]
       (assoc m k template)))
   {}
   cfg))

;; # Config Reader Literals

(defn- kv-params->aws-params
  "Returns AWS parameter array based on key-value param map `m`."
  [m]
  (into [] (for [[k v] m] {:ParameterKey (name k) :ParameterValue v})))

(defn- k->aws-resource-ref 
  "Return AWS logical resource reference based on resource key `k`."
  [k]
  {:Ref (name k)})

(defn- with-aws-ssm-param-resources
  "Return resource map `m` with a Sytem Manager Parameters merged in for each resource."
  [m]
  (reduce-kv
   (fn [acc k _]
     (assoc acc
            (keyword (str (name k) "Param"))
            [:SSM.Parameter {:Name (name k) :Value (k->aws-resource-ref k) :Type "String"}]))
   m
   m))

(defn create-readers 
  "Returns edn reader literals, expects `env` keyword and `param` map.
   Where appropriate we match the AWS function utilities provided for YAML/JSON templates."
  [env params]
  {'eid (fn [k] (eid k env))
   'kvp kv-params->aws-params
   'ref k->aws-resource-ref
   'sub (fn [k] (get params k))
   'with-ssm-params with-aws-ssm-param-resources})

;; # Config Reader

;; TODO prefix resources with clf 
;; TODO get system parameters
(defn read-edn
  "Return serialized AWS config based edn string `s`, environment keyword `env` and parameter map `params`.
   See `serialize-config` for templating details."
  ([s env] (read-edn s env {}))
  ([s env params]
   (serialize-config (edn/read-string {:readers (create-readers env params)}
                                      s))))
