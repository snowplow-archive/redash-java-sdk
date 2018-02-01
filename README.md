# redash-java-sdk

[![Build Status][travis-image]][travis] [![Coveralls][coveralls-image]][coveralls] [![Release][release-image]][releases] [![License][license-image]][license]

## Overview

This library can be used to interact with your Redash server on a programatic level.  The API functions it currently exposes are:

1. Create new data-source
2. Update specific data-source
3. List all data-sources
4. Delete specific data-source
5. Create new user-group
6. Add user to specific user-group(s)
7. Add data-source to specific user-group(s)
8. Remove user from specific user-group(s)
9. Remove data-source from specific user-groups(s)
10. List all user-groups
11. Delete specific user-group
12. List all users
13. Get specific user
14. Get specific data-source

Let us know if there are other endpoints you would like to see added here!

### Documentation

Please see the [Javadoc][techdocs] for help in understanding the API or the [Setup Guide][setup] for help on integrating the library.

### Compatibility

This library has been tested with the following versions of Redash:

* 3.0.0+b3134

## Quickstart

Assuming git, **[Vagrant][vagrant-install]** and **[VirtualBox][virtualbox-install]** installed:

```bash
 host$ git clone https://github.com/snowplow-incubator/redash-java-sdk.git
 host$ cd redash-java-sdk
 host$ vagrant up && vagrant ssh
guest$ cd /vagrant
```

The tests require a local Redash server be setup and configured in a certain way.  This setup has been fully automated and added as a custom gradle task `redashSetup` which is always run before running `test`.  This task does several things:

1. Launches the 5 docker containers required for redash
2. Configures an admin and default user
3. Extracts the admin users API key
4. Populates a dynamic properties resource within `src/test/resources/redash_dynamic.properties`

__Note__: If there are any issues with this setup you can first try resetting the setup via `./gradlew redashDestroy` and then rerunning the `./gradlew redashSetup`.

```
guest$ ./gradlew clean build
guest$ ./gradlew test
```

## Copyright and license

The Redash Java SDK is copyright 2018 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[travis]: https://travis-ci.org/snowplow-incubator/redash-java-sdk
[travis-image]: https://travis-ci.org/snowplow-incubator/redash-java-sdk.svg?branch=master

[release-image]: http://img.shields.io/badge/release-0.1.0-6ad7e5.svg?style=flat
[releases]: https://github.com/snowplow-incubator/redash-java-sdk/releases

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[coveralls-image]: https://coveralls.io/repos/github/snowplow-incubator/redash-java-sdk/badge.svg?branch=master
[coveralls]: https://coveralls.io/github/snowplow-incubator/redash-java-sdk?branch=master

[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads

[techdocs]: https://snowplow-incubator.github.io/redash-java-sdk/
[setup]: https://github.com/snowplow-incubator/redash-java-sdk/wiki/Redash-Java-SDK-Setup
