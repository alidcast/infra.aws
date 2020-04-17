# Infra.aws (alpha)

Infra helps you configure Cloudformation templates using EDN and manage the configured stack's resources using Clojure.

*Note: Currently you can only create resources via the repl but that still beats having to create resources using the aws console or having to define configurations in YAML/JSON files. In the future Infra will likely provide a cli for creating cloudformation stacks.*

## Preview 

Imagine you're setting up AWS Cognito for user authentication.

First configure your Cloudformation's stack template in EDN using the shorthands and reader literal we provide:

```clj
;; resources/infra-aws-stacks.edn
 
 #eid :app-auth ;; Make the stack identifier unique per environment
 {:Resources 
 ;; Configure Cognito User Pool and Pool Client Resources
 ;; Automatically including a System Manager Parameters for each resource.
  #with-ssm-params
   {:AuthPool    [:Cognito.UserPool
                  {:UserPoolName  #eid :app-auth-pool
                  :AutoVerifiedAttributes ["email"]
                  :Schema  [{:Name               "email"
                              :AttributeDataType  "String"
                              :Required           true
                              :Mutable            true}]}]
  :AuthPoolClient [:Cognito.UserPoolClient
                  {:ClientName       #eid :app-auth-pool-client
                    :UserPoolId       #ref :AuthPool
                    :ExplicitAuthFlows ["ALLOW_USER_PASSWORD_AUTH"
                                        "ALLOW_REFRESH_TOKEN_AUTH"]}]}}
```


Then, setup your stack via the repl:

```clj
(ns app.infra.provider
  (:require [rejure.infra.aws.config :as ic]
            [rejure.infra.aws.request :as ir]))

;; Read the config for the :dev environment
(def cfg (ic/read-edn (io/reader "resources/infra-aws-stacks.edn") :dev))

(defn get-stack [k] (get cfg (ic/eid k :dev)))

(comment
  ;; Manage the app's authentication service stacks via the repl
  (ir/create-stack (get-stack :app-auth))
  (ir/delete-stack (get-stack :app-auth))

  ;; Get list of all auto-configured System Parameter keys.
  ;; You can later retrieve them using aws-api :ssm client.
  (ic/get-ssm-param-keys cfg)
)
```

## Rationale 

Infra, motivation:

1. There isn't reliable way to manage AWS infrastructure using Clojure. The default solution is to use the AWS cli and write Cloudformation templates with JSON/YAML, though one could also choose an infrastructure-as-code provider such as Terraform (which provides their own templating DSL) or Pulumi (which lets you configure templates via code but does not support Clojure). But having your configurations in another language creates a layer of separation between your application and its infrastructure and demands more work keeping both in sync.
2. Cloudformation templates options are verbose and existing Clojure libraries that make them easier to write expect you to do so as code rather than as data in EDN files.
3. No straightforward way to handling different environments and secrets that are exchanged to-and-from an application and its infrastructure.

Design goals:
1. Make AWS template configurations explicit, taking advantage of EDN and reader literals.
2. Provide a way to manage different environments with a single configuration.
3. Provide tools for setting up and inspecting Cloudformation stacks.
4. Match the templating options and functions AWS already provides where convenient.

## Status 

Alpha. Clojar has not been deployed.

## Usage

- [Configuring Resources](#configuring-resources)
  - [Resource Types](#resource-types)
  - [Writing Templates](#writing-templates)
- [Creating resources](#creating-resources)

This documentation assumes that you're familiar with [AWS Cloudformation](https://docs.aws.amazon.com/cloudformation/index.html), though you'll mostly just need to reference the [resource property options](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html) for the resources you'll be creating.

Note: AWS Credentials are looked up according to AWS's [Java SDK](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html). So either set your `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` credentials as env variables or configure them in the designated `~/.aws/credentials`.

### Configuring Resources

Your edn file must be a mapping of resource names to their template bodies.

#### Resource Types

A template body can either be a vector, if a template is derived form a url, or a map.

**Template Urls**

Since [AWS Cloudformation Stack options](https://docs.aws.amazon.com/AWSCloudFormation/latest/APIReference/API_CreateStack.html) are quite verbose, we have a shorthand for defining a template urls. You can declare a tuple where the first arg is the template url and the second is AWS Stack options:

```clj
{:MyStack ["http:/s3.amazonaws.com/path-to-template"
           {:Parameters []}]}
;; {:MyStack {:StackName   "MyStack"
;;            :TemplateURL "http:/s3.amazonaws.com/path-to-template"
;;            :Parameters  []}}
```

**Custom Templates**

To declare custom template maps you can pass the [AWS template options](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-anatomy.html) directly. 

For declaring [Resources](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/resources-section-structure.html), we provide a shorthand you can use. Instead of a map with `type` and `properties` key-values, can pass a tuple where the first arg is the resource identifier type keyword (separating the service and its respective module with a period) and the second is its properties.

```clj
{:MyStack 
 {:Resources {:ExResource [:Service.Module
                           {:Name "Foo"}]}}}
;; {:MyStack 
;;  {:Resources {:ExResource [{:Type "AWS::Service::Module"
;;                             :Properties {:Name "Foo"}}]}}}
```

#### Writing Templates

Infra provides a few template literals to make it easier to markup your templates.

AWS has [intrinsic function utilities](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference.html) it uses for marking up JSON. If a reader literal serves a similar purpose as an existing utility we name it after its JSON counterpart.

You can look at the [example configs](https://github.com/rejure/infra.aws/tree/master/example/resources/demo)s to see the template literals listed below in action. 

List of available reader literals: 

* `eid`: append environment info to an identifier string. Useful for naming resources per environment.

* `kvp`: convert key-value parameter map to aws parameter array.

* `ref`: reference a resource by its [logical id](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/resources-section-structure.html).

* `sub`: substitute a keyword with a parameter passed to configuration reader.

### Creating resources

Right now you can only create resources via the repl - but that still beats having to create them via aws console.

Note: If you're creating resources that depend on one another make sure you wait for the given dependencies to finish creating.

Example: 

```clj
(def env :dev)
(def params {})

(def cfg (ic/read-config "demo/aws-config.edn" env params))

(defn get-stack [k]
  (get cfg (ic/eid k env)))

(comment 
 (ir/create-stack (get-stack "Stack1"))
 (ir/create-stack (get-stack "Stack2"))
 )
```

List of available operations: 

* `create-stack` 
* `delete-stack` 

## References

Infra is built on Clojure's [edn](https://github.com/edn-format/edn) and Cognitecht's [aws-api](https://github.com/cognitect-labs/aws-api).

Infra is inspired by [aero](https://github.com/juxt/aero), which we originally used but since AWS already provides templating functions we decided to use EDN directly and have most custom reader literals match the AWS templating spec.

## Acknowledgments

Thanks to Joe Lane for discussing Code-As-Infrastructure with me and convincing me not to use Pulumi+Typescript.

## License

MIT © [Alid Lorenzo](https://github.com/alidlo)
