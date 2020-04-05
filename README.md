# Infra

Manage AWS infrastructure with Clojure and EDN.

Infra builds upon Cognitecht's [aws-api](https://github.com/cognitect-labs/aws-api) to create AWS stacks and Juxt's [aero](https://github.com/juxt/aero) to serialize EDN configuration files into cloudformation templates.

## Docs

Your edn file must be a mapping of resource names to their template bodies.

A template body can either be a vector, if a template is derived form a url, or a map.

### Template Urls

Since [AWS cloudformation options](https://docs.aws.amazon.com/AWSJavaScriptSDK/latest/AWS/CloudFormation.html#createStack-property) are quite verbose, we have a shorthand for defining a template url and its parameters.

To declare a template url, create a vector where the first arg is the template url and the second is a parameter map, and the third is optional aws cloudformation options. The key-value parameters will be serialized to match AWS spec and merged with the extra aws options.

```
{:template-url-ex ["http:/s3.amazonaws.com/path-to-template"
                   {:Param1 "Value"}]}
// {:StackName   "template-url-ex"
//  :TemplateURL "http:/s3.amazonaws.com/path-to-template"
//  :Parameters  [{ParameterKey: "Param1", ParameterValue: "Value"]}
```

### Custom Templates

To declare custom templates just pass the [AWS cloudformation options](https://docs.aws.amazon.com/AWSJavaScriptSDK/latest/AWS/CloudFormation.html#createStack-property) directly.
