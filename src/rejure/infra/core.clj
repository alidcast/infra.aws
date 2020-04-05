(ns rejure.infra.core
  "Serialize edn to cloudformation template."
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.edn :as edn]))

(defn eid 
  "Returns identifier key based on environment.
   Takes `ident` keyword and `env` keyword as arguments."
  [ident env]
  (str (name ident) "-" (name env)))

;; # Config reader 

(defn- url-template? [v]
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
   The key-value parameters are serialized to match aws spec and merged with the extra aws options.
   -
   Note: We only need to serialize nested properties to JSON, the rest are handled by aws-api client."
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
                         :TemplateBody (json/write-str v)})]
       (assoc m k template)))
   {}
   template))

(defn load-edn
  "Load edn from an io/reader resource `path`."
  [path opts]
  (try
    (with-open [stream (java.io.PushbackReader. (io/reader (io/resource path)))]
      (edn/read opts stream))
    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" path (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" path (.getMessage e)))))

(defn create-readers [env params]
  {'eid (fn [k] (eid k env))
   'ref (fn [k] {:Ref (name k)})
   'sub (fn [k] (get params k))})

(defn read-config
  "Read an AWS resource configuration.
   Takes a resource path `path`, env keyword `env` and parameter map `params` as arguments.
   Returns mapping of resources with their template options serialized to match aws spec.
   See `serialize-config` for templating details."
  ([path env] (read-config path env {}))
  ([path env params]
   (serialize-config (load-edn path {:readers (create-readers env params)}))))
