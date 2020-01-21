# Pusher Chatkit Android

[![Read the docs](https://img.shields.io/badge/read_the-docs-92A8D1.svg)](https://docs.pusher.com/chatkit/reference/android)
[![Twitter](https://img.shields.io/badge/twitter-@Pusher-blue.svg?style=flat)](http://twitter.com/Pusher)
[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg)](https://raw.githubusercontent.com/pusher/chatkit-android/master/LICENSE)
[![codecov](https://codecov.io/gh/pusher/chatkit-android/branch/master/graph/badge.svg)](https://codecov.io/gh/pusher/chatkit-android)
[![Travis branch](https://img.shields.io/travis/pusher/chatkit-android/master.svg)](https://travis-ci.org/pusher/chatkit-android)
[![Download](https://api.bintray.com/packages/pusher/maven/chatkit-android/images/download.svg)](https://bintray.com/pusher/maven/chatkit-android/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/com.pusher/chatkit-android.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.pusher%22%20AND%20a:%22chatkit-android%22)

Find out more about Chatkit [here](https://pusher.com/chatkit).

The SDK is written in Kotlin, but aimed to be as Java-friendly as possible.
Please report incompatibilities with Java as bugs.

## Requirements

- minSdkVersion is 19 (KitKat)

## Installation

The project is hosted primarily on JCenter, and synced to Maven Central.

The latest release version can be seen in the badges above.

## Deprecated versions

Versions of the library below
[1.0.0](https://github.com/pusher/chatkit-android/releases/tag/v1.0.0) are no
longer supported by the backend.

To view a list of changes, please refer to the [CHANGELOG](CHANGELOG.md).

### Gradle

```
repositories {
  jcenter()
}

dependencies {
  // ...
  implementation 'com.pusher:chatkit-android:$chatkit-version'
}
```

### Maven

```
<!-- optional, add the primary source repo.
     exclude to use mirrored version from Maven Central -->
<repositories>
  <repository>
    <id>jcenter</id>
    <url>https://jcenter.bintray.com/</url>
  </repository>
<repositories>

<dependency>
  <groupId>com.pusher</groupId>
  <artifactId>chatkit-android</artifactId>
  <version>VERSION</version>
</dependency>
```

## Development

When building this project, you may choose to use a local version of
[`pusher-platform-android`](1).

To do so you can add the path to your local copy in your
`~/.gradle/gradle.properties`:

```
pusher_platform_local=/path/to/pusher-platform-android
```

It can be either a relative or absolute path.

[1]: https://github.com/pusher/pusher-platform-android

## Testing

The SDK has integration tests which run against a real Chatkit server.

Firstly you will need to install the
[Spek](https://www.spekframework.org/setup-android/) plugin, and Spek Framework in
Android Studio. To do this go to Android Studio -> Preferences -> Plugins ->
search for Spek, and Spek Framework and install. If you open a Spek test file, you
should now see green play buttons to run each test (or the file).

In order for the tests to pass you must provide a Chatkit instance
credential to the VM - to do this edit the run configurations -> select Spek
on the left -> in the VM options field enter the following:

```
-Dchatkit_integration_locator=xxxx -Dchatkit_integration_key=yyy
```

*Important:* The tests will delete any and all resources associated with
the instance you provide. Create a new instance for testing purposes, and do not
share it anywhere else.

## Publishing

### jCenter

The two artifacts this project produces (`chatkit-core` and `chatkit-android`)
are published in `jCenter`.

Firstly, make sure you have a [BinTray](https://bintray.com) account. To get
the api key go to Profile > Edit > Api Key

Then you need to set up a user name and api key.

Either on your local `~/.gradle/gradle.properties` as:

```properties
bintrayUser=you-bintray-user-name
bintrayApiKey=your-bintray-api-key
```

Or as environment variables (mainly for CI):

```bash
BINTRAY_USER=you-bintray-user-name
BINTRAY_API_KEY=your-bintray-api-key
```

You will also need to have `SONATYPE_GPG_PASSPHRASE` set as an environment
variable. This is, as the name suggests, the GPG passphrase for the Maven
signing key.

Now, to do the actual release run:

```bash
gradlew build
gradlew bintrayUpload
```

**Note:** The publish action will both override the current release (if it has
the same version name as the current) and automatically publish the new
version.

### Maven

You should sync the artefacts to Maven from the Bintray web interface.
