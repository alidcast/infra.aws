# Infra.aws (alpha)

Motivation for [infra.aws](https://github.com/alidlo/infra.aws/tree/master/infra.aws)
* Standardize a succinct, declarative way to write AWS Cloudformation templates using EDN.
  * Matching the templating options and functions AWS already has, providing shorthands where useful.
* Provide a way to manage different environments with a single configuration.
* Provide utilities for setting up and inspecting Cloudformation stacks.

*Note: Currently you can only create resources via the repl but that still beats using the aws console or having to define configurations in YAML/JSON files.*

Example: 

```clj
;; resources/aws-stacks.edn
{#eid :app-auth ;; Make the stack identifier unique per environment
 {:Resources 
 ;; Configure AWS Cognito UserPool and UserPoolClient Resources
 ;; Automatically including a System Manager Parameters for each resource identifier.
  #aws/with-ssm-params
   {:AuthPool    [:Cognito.UserPool
                  {:UserPoolName  #eid :app-auth-pool}]
  :AuthPoolClient [:Cognito.UserPoolClient
                  {:ClientName       #eid :app-auth-pool-client
                   :UserPoolId      #aws/ref :AuthPool}]}}}


;; Read the config for the :dev environment
(require '[infra.aws.config :as ic])
(def cfg (ic/read-edn (io/reader "resources/aws-stacks.edn") :dev))
(defn get-stack [k] (get cfg (ic/eid k :dev)))

;; Manage the resources in the repl 
(require '[infra.aws.request :as ir])
(comment
  ;; Manage the app's authentication service stacks via the repl
  (ir/create-stack (get-stack :app-auth))
  (ir/delete-stack (get-stack :app-auth))
  ;; Get list of all auto-configured System Parameter keys.
  ;; You can later retrieve them using aws-api :ssm client.
  (ic/get-ssm-param-keys cfg))
```

## Usage 

See respective projects README for usage.

## License

MIT © [Alid Lorenzo](https://github.com/alidlo)
