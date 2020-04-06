(ns rejure.infra.aws.config
  "Configure cloudformation templates via EDN."
  (:require 
  ;;  [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.string :as string]))

;; # Config Helpers

(defn eid 
  "Returns identifier key based on environment.
   Takes `ident` keyword and `env` keyword as arguments."
  [ident env]
  (str (name ident) "-" (name env)))


;; # Config Reader 

(defn- template-url? [v]
  (vector? v))

(defn- ->template-url-map
  "Accepts stack name keyword `k` and template `url` string."
  [k url]
  {:StackName   k
   :TemplateURL url})

(defn- ->resource-type
  "Converts resource key `k` to AWS identifier type.
   The key should be in ':<Service>.<Module>' format.
   i.e. :Service.Module -> AWS::Service::Module"
  [k]
  (string/join "::" (cons "AWS" (string/split (name k) #"\.")))
  )

(defn- ->template-body-map
  "Accepts stack name keyword `k` and template `body`."
  [k body]
  {:StackName    k
   :TemplateBody (assoc body 
                        :Resources 
                        (reduce-kv (fn [rm rk rv]
                                     (assoc rm  rk (if (vector? rv)
                                                     {:Type (->resource-type (first rv))
                                                      :Properties (last rv)}
                                                     rv)))
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
                          (merge (->template-url-map k url)
                                 clf-opts))
                        (->template-body-map k v))]
       (assoc m k template)))
   {}
   config))

(defn- kv-params->aws-params
  "Convert paramter map to AWS paramater array."
  [params]
  (into [] (for [[k v] params] {:ParameterKey (name k) :ParameterValue v})))

(defn create-readers 
  "Create edn reader literals.
   Where appropriate we match the AWS function utilities provided for YAML/JSON templates.
   (ref: https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference.html)"
  [env params]
  {'eid (fn [k] (eid k env))
   'kvp (fn [m] (kv-params->aws-params m))
   'ref (fn [k] {:Ref (name k)})
   'sub (fn [k] (get params k))})

(defn read-edn
  "Read an AWS edn configuration.
   Takes an edn string `s`, env keyword `env` and parameter map `params` as arguments.
   Returns mapping of resources with their template options serialized to match aws spec.
   See `serialize-config` for templating details."
  ([s env] (read-edn s env {}))
  ([s env params]
   (serialize-config (edn/read-string {:readers (create-readers env params)}
                                      s))))
