(ns infra.aws.configure-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.edn :as edn]
            [infra.aws.configure :as ic]))

(deftest test-create-readers
  (let [f #(edn/read-string {:readers (ic/create-readers :dev {})} %)]
    (testing "#aws/params converts key-value parameters to aws parameter array"
      (is (= (f "#aws/params {:Key1 \"Value1\"}")
             [{:ParameterKey "Key1"
               :ParameterValue "Value1"}])))

    (testing "#aws/with-ssm-params adds system parameters for each resource identifier"
      (is (= (f "{:Stack {:Resources #aws/with-ssm-params {:Foo {}}}}")
             {:Stack {:Resources
                      {:Foo {}
                       :FooParam {:Type "AWS::SSM::Parameter"
                                  :Properties {:Name "Foo"
                                               :Type  "String"
                                               :Value {:Ref "Foo"}}}}}})))))

(deftest test-serialize-config
  (let [f #(ic/serialize-config {:cloudformation/stacks %})]
    (testing "case: template url"
      (testing "expands tuple shorthand into aws option map"
        (is (= (f {:Stack ["http:s3.example.com/template.json"
                           {:Capabilities ["ROLE_IAM"]}]})
               {:Stack {:StackName   :Stack
                        :TemplateURL "http:s3.example.com/template.json"
                        :Capabilities ["ROLE_IAM"]}}))))

    (testing "case: template body"
      (let [result {:Stack {:StackName   :Stack
                            :TemplateBody {:Resources {:Foo {:Type "AWS::Service::Module"
                                                             :Properties {:Name "foo-bar"}}}}}}]
        (testing "aws options map are left untouched"
          (is (= (f {:Stack {:Resources {:Foo {:Type "AWS::Service::Module"
                                                :Properties {:Name "foo-bar"}}}}})
                 result)))
        (testing "tuple shorthand is expanded into aws option map"
          (is (= (f {:Stack {:Resources {:Foo [:AWS.Service/Module
                                               {:Name "foo-bar"}]}}})
                 result)))))))
