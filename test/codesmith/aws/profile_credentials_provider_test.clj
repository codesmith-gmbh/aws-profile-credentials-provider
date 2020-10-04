(ns codesmith.aws.profile-credentials-provider-test
  "To run the tests, you need to create a profile with the name `role-based-profile-credentials-provider-test` that
  assumes a role with the ListBuckets IAM permission."
  (:require [clojure.test :refer :all]
            [cognitect.aws.config :as config]
            [codesmith.aws.profile-credentials-provider :as pcp]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.region :as region]))

(deftest profile-credentials-provider-correctness
  (let [profile-name "role-based-profile-credentials-provider-test"
        role-arn     (get-in (config/parse (pcp/scan-for-credentials-file))
                             [profile-name "role_arn"])
        s3-client    (aws/client {:api                  :s3
                                  :credentials-provider (pcp/profile-credentials-provider profile-name)
                                  :region-provider      (region/profile-region-provider profile-name)})]
    (testing (str "the profile " profile-name " is role based.")
      (is role-arn))
    (testing (str "using credentials from the profile " profile-name)
      (let [response (aws/invoke s3-client {:op :ListBuckets})]
        (is (:Buckets response))
        (is (not (:cognitect.anomalies/category response)))))))