# Infra

Manage AWS infrastructure with Clojure and EDN.

This documentation assumes that you're familiar with [AWS cloudformation](https://docs.aws.amazon.com/cloudformation/index.html), though you mostly just need to reference the [resource property options](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html) for the resources you'll be creating.

## Docs


### Configure Resources

Your edn file must be a mapping of resource names to their template bodies.

#### Resource Types
A template body can either be a vector, if a template is derived form a url, or a map.

**Template Urls**

Since [AWS cloudformation options](https://docs.aws.amazon.com/AWSCloudFormation/latest/APIReference/API_CreateStack.html) are quite verbose, we have a shorthand for defining a template url and its parameters.

To declare a template url, create a vector where the first arg is the template url and the second is a parameter map, and the third is optional aws cloudformation options. The key-value parameters will be serialized to match AWS spec and merged with the extra aws options.

```clj
{:template-url-ex ["http:/s3.amazonaws.com/path-to-template"
                   {:Param1 "Value"}]}
;; {:StackName   "template-url-ex"
;;  :TemplateURL "http:/s3.amazonaws.com/path-to-template"
;;  :Parameters  [{ParameterKey: "Param1", ParameterValue: "Value"]}
```

**Custom Templates**

To declare custom templates just pass the [AWS cloudformation options](https://docs.aws.amazon.com/AWSCloudFormation/latest/APIReference/API_CreateStack.html) directly.

#### Writing Templates

Infra provides a few template literals to make it easier to markup your templates.

AWS has [intrinsic function utilities](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference.html) it uses for marking up JSON. If a reader literal serves a similar purpose as an existing utility we name it after its JSON counterpart.

You can look at the [examples](https://github.com/rejure/infra/tree/master/examples/datomic-api) directory to see template literals in action. But here's a contrived example for your convenience:

```clj
{#eid Stack1
 {:Resources {:Resource1 {:Type          "AWS::Service::Module"
                          :Properties   {:ServiceName #eid :OrgResource1}}
              :Resource2 {:Type          "AWS::Service::ModuleClient"
                          :Properties   {:ServiceName #eid :OrgResource2
                                         :ReferenceId #ref :Resource1}}}}
```

List of available reader literals: 

* `eid`: append environment info to an identifier string. Useful for naming resources per environment.

* `ref`: reference a resource by its [logical id](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/resources-section-structure.html).

* `sub`: substitute a keyword with a parameter passed to configuration reader.


### Creating resources

Right now you can only create resources via the repl - but that still beats having to creating them via aws console.

Note: If you're creating resources that depend on one another make sure you wait for the given dependencies to finish creating.

```clj
(def env :dev)
(def params {})

(def cfg (infra.core/read-config "demo/aws-config.edn" env params))

(defn get-stack [k]
  (get cfg (infra/eid k env)))

(comment 
 (infra.aws/create-stack (get-stack "Stack1"))
 (infra.aws/create-stack (get-stack "Stack2"))
 )
```

