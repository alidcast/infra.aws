(ns rejure.infra.aws.config-test
  (:require [clojure.test :refer [deftest testing is]]
            ;; [clojure.data.json :as json]
            [rejure.infra.aws.config :as icfg]))

(deftest read-edn-test
  (let [read-edn #(icfg/read-edn % :test)]

    (testing "serialize config properties to aws spec"
      (testing "template url"
        (testing "exapnds tuple shorthand into aws option map"
         (is (= (read-edn (pr-str {:Foo ["http:s3.example.com/template.json"
                                         {:Capabilities ["ROLE_IAM"]}]}))
                {:Foo {:StackName   :Foo
                       :TemplateURL "http:s3.example.com/template.json"
                       :Capabilities ["ROLE_IAM"]}}))))

      (testing "template body"
        (let [result {:Foo {:StackName   :Foo
                            :TemplateBody {:Resources {:Foo {:Type "AWS::Service::Module"
                                                             :Properties {:Name "foo-bar"}}}}}}]
          (testing "aws options map are left untouched"
            (is (= (read-edn (pr-str {:Foo {:Resources {:Foo {:Type "AWS::Service::Module"
                                                              :Properties {:Name "foo-bar"}}}}}))
                   result)))
          (testing "tuple shorthand is expanded into aws option map"
            (is (= (read-edn (pr-str {:Foo {:Resources {:Foo [:Service.Module
                                                              {:Name "foo-bar"}]}}}))
                   result))))))

    (testing "config reader literals"
      (testing "#kvp converts key-value parameters to aws parameter array"
        (is (= (read-edn "{:Foo [\"url\" {:Parameters #kvp {:Key1 \"Value1\"}}]}")
               {:Foo {:StackName    :Foo
                      :TemplateURL  "url"
                      :Parameters [{:ParameterKey "Key1"
                                    :ParameterValue "Value1"}]}}))))))
