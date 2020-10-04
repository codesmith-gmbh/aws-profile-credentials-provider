# Codesmith AWS Profile Credentials Provider

A profile credentials provider library for [cognitect-labs/aws-api](https://github.com/cognitect-labs/aws-api).

This library is based on the [cognitect-labs/aws-api](https://github.com/cognitect-labs/aws-api) AWS Api
library.

## Usage

Assuming an `~/.aws/credentials` file with the following content:

```ini
[role-based-profile]
source_profile = base-profile
role_arn = arn:aws:iam::123456789012:role/RoleName

[base-profile]
aws_access_key_id     = ZZZZZZZZZZZZZZZZZZZZ
aws_secret_access_key = ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ
```

You can use the provided profile credentials provider to use the profile `role-based-profile`; to use the region
configured for the profile, you typically use the `profile-region-provider` in the namespace `cognitect.aws.region`.

```clojure
(require '[cognitect.aws.client.api :as aws])
(require '[cognitect.aws.region :as region])
(require '[codesmith.aws.profile-credentials-provider :as pcp])

(def profile-name "the-profile")

(def s3-client (aws/client {:api                  :s3
                            :credentials-provider (pcp/profile-credentials-provider profile-name)
                            :region-provider      (region/profile-region-provider profile-name)}))
```

The library use AWS STS to retrieve temporary credentials.

## License

Copyright © 2020 Codesmith GmbH (adaptation)  
Copyright © 2015 — 2020 Cognitect (original code and example, licensed under the Apache License, Version 2.0)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

```
http://www.apache.org/licenses/LICENSE-2.0
```

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
