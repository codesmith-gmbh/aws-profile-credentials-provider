(ns codesmith.aws.profile-credentials-provider
  "The API for this namespace is the function [[profile-credentials-provider]].
  It allows to use CLI named profiles that are base on a role to be assumed and a source profile.
  It builds upon the `cognitect.aws.credentials` namespace and the example found at
  https://github.com/cognitect-labs/aws-api/blob/master/examples/assume_role_example.clj"
  (:require [clojure.java.io :as io]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.config :as config]
            [clojure.tools.logging :as log]
            [cognitect.aws.credentials :as creds]
            [cognitect.aws.region :as region])
  (:import [java.io File]
           [java.util UUID]))

(defn scan-for-aws-profile-name []
  "Scans different locations for the name of cli profile to be used.
  https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
  cf. com.amazonaws.auth.profile.internal.AwsProfileNameLoader"
  (or (System/getenv "AWS_PROFILE")
      (System/getProperty "aws.profile")
      "default"))

(defn scan-for-credentials-file []
  "Scans different locations for the credentials file.
  https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
  cf. com.amazonaws.profile.path.AwsProfileFileLocationProvider"
  (or (io/file (System/getenv "AWS_CREDENTIAL_PROFILES_FILE"))
      (io/file (System/getProperty "user.home") ".aws" "credentials")))

(def no-credentials-provider
  "A credential provider that gives no credentials."
  (reify creds/CredentialsProvider
    (fetch [_])))

(defn assume-role-from-profile-provider [role-arn source-profile f]
  "A credential provider that assumes the givel role via the source profile
  Arguments:
  role-arn        string   The arn of the role to assume.
  source-profile  string   The profile to use to assume the role.
  f               File     The credential file to scan for the profile.

  The base credential provider is wrapped with an auto refresh provider to update
  the temporary credentials"
  (creds/cached-credentials-with-auto-refresh
    (reify creds/CredentialsProvider
      (fetch [_]
        (try
          (let [sts-client        (aws/client {:api                  :sts
                                               :credentials-provider (creds/profile-credentials-provider source-profile f)
                                               :region-provider      (region/profile-region-provider source-profile)})
                role-session-name (str "aws-api-role-profile-creds-" (UUID/randomUUID))
                aws-request       {:op      :AssumeRole
                                   :request {:RoleArn         role-arn
                                             :RoleSessionName role-session-name}}
                invocation        (aws/invoke sts-client aws-request)
                {:keys [AccessKeyId
                        SecretAccessKey
                        SessionToken]
                 :as   creds} (:Credentials invocation)]
            (creds/valid-credentials
              {:aws/access-key-id     AccessKeyId
               :aws/secret-access-key SecretAccessKey
               :aws/session-token     SessionToken
               ::creds/ttl            (creds/calculate-ttl creds)}
              "credentials file"))
          (catch Throwable t
            (log/error t
                       "Error fetching credentials from source profile {} and role arn {}"
                       source-profile
                       role-arn)))))))

(defn profile-credentials-provider
  "Return credentials in an AWS configuration profile.
  Arguments:
  profile-name  string  The name of the profile in the file. (default: default)
  f             File    The profile configuration file. (default: ~/.aws/credentials)
  https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
    Parsed properties (2 set)
    1. aws_access_key        required
       aws_secret_access_key required
       aws_session_token     optional
    2. role_arn              required
       source_profile        required"
  ([]
   (profile-credentials-provider (scan-for-aws-profile-name)))
  ([profile-name]
   (profile-credentials-provider profile-name (scan-for-credentials-file)))
  ([profile-name ^File f]
   (try
     (let [credentials (config/parse f)
           {:strs [role_arn
                   source_profile]} (get credentials profile-name)]
       (if (and role_arn source_profile)
         (assume-role-from-profile-provider role_arn source_profile f)
         (creds/profile-credentials-provider profile-name)))
     (catch Throwable t
       (log/error t
                  "Error fetching credentials from file {}"
                  f)
       no-credentials-provider))))
