(ns infra.aws.config-test
  (:require [clojure.test :refer [deftest testing is]]
            ;; [clojure.data.json :as json]
            [infra.aws.config :as icfg]))

(deftest read-edn-test
  (let [read-edn #(icfg/read-edn % :test)]

    (testing "serialize config properties to aws spec"
      (testing "case: template url"
        (testing "expands tuple shorthand into aws option map"
          (is (= (read-edn (pr-str {:Stack ["http:s3.example.com/template.json"
                                            {:Capabilities ["ROLE_IAM"]}]}))
                 {:Stack {:StackName   :Stack
                          :TemplateURL "http:s3.example.com/template.json"
                          :Capabilities ["ROLE_IAM"]}}))))

      (testing "case: template body"
        (let [result {:Stack {:StackName   :Stack
                              :TemplateBody {:Resources {:Foo {:Type "AWS::Service::Module"
                                                               :Properties {:Name "foo-bar"}}}}}}]
          (testing "aws options map are left untouched"
            (is (= (read-edn (pr-str {:Stack {:Resources {:Foo {:Type "AWS::Service::Module"
                                                                :Properties {:Name "foo-bar"}}}}}))
                   result)))
          (testing "tuple shorthand is expanded into aws option map"
            (is (= (read-edn (pr-str {:Stack {:Resources {:Foo [:Service.Module
                                                                {:Name "foo-bar"}]}}}))
                   result))))))

    (testing "interprets config reader literals correctly"
      (testing "#kvp converts key-value parameters to aws parameter array"
        (is (= (read-edn "{:Stack [\"url\" {:Parameters #aws/kvp {:Key1 \"Value1\"}}]}")
               {:Stack {:StackName    :Stack
                        :TemplateURL  "url"
                        :Parameters [{:ParameterKey "Key1"
                                      :ParameterValue "Value1"}]}})))

      (testing "#with-ssm-params adds system parameters for each resource identifier"
        (is (= (read-edn "{:Stack {:Resources #aws/with-ssm-params {:Foo {}}}}")
               {:Stack {:StackName    :Stack
                        :TemplateBody {:Resources
                                       {:Foo {}
                                        :FooParam {:Type "AWS::SSM::Parameter"
                                                   :Properties {:Name "Foo"
                                                                :Value {:Ref "Foo"}
                                                                :Type  "String"}}}}}}))))))
