# Infra Usage

Tools for managing AWS cloudformation resources.

Some considerations:
* Matching the templating options and functions AWS already has, providing shorthands where useful.
* Provide a way to manage different environments with a single configuration.
* Provide utilities for setting up and inspecting Cloudformation stacks.

## Usage 

- [Configuring Resources](#configuring-resources)
  - [Resource Types](#resource-types)
  - [Writing Templates](#writing-templates)
- [Managing Resources](#managing-resources)

This documentation assumes that you're familiar with [AWS Cloudformation](https://docs.aws.amazon.com/cloudformation/index.html), though you'll mostly just need to reference the [resource property options](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html) for the resources you'll be creating.

Note: AWS Credentials are looked up according to AWS's [Java SDK](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html). So either set your `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` credentials as env variables or configure them in the designated `~/.aws/credentials`.

### Configuring Resources


Configure all cloudformation stacks under the `:cloudformation/stacks` property.

Each option is a mapping of stack names to their resource template configurations.

```clj
{:cloudformation/stacks  
 {:MyStack {,,,}}}
```

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
 {:Resources {:ExResource [:AWS.Service/Module
                           {:Name "Foo"}]}}}
;; {:MyStack 
;;  {:Resources {:ExResource [{:Type "AWS::Service::Module"
;;                             :Properties {:Name "Foo"}}]}}}
```

#### Writing Templates

Infra provides a few template literals to make it easier to markup your templates.

AWS has [intrinsic function utilities](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference.html) it uses for marking up JSON. If a reader literal serves a similar purpose as an existing utility we name it after its JSON counterpart.

You can look at the [examples][(https://github.com/rejure/infra.aws/tree/master/example/resources/demo)s to see the template literals listed below in action. 

List of available reader literals: 

* `eid`: append environment info to an identifier string. Useful for naming resources per environment.

* `aws/params`: convert key-value parameter map to aws parameter array.

* `aws/ref`: reference a resource by its [logical id](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/resources-section-structure.html).

* `aws/sub`: substitute a keyword with a parameter passed to configuration reader.

### Managing Resources

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
