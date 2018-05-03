# chatkit-android

[![Twitter](https://img.shields.io/badge/twitter-@Pusher-blue.svg?style=flat)](http://twitter.com/Pusher)
[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg)](https://raw.githubusercontent.com/pusher/chatkit-android/master/LICENSE)
[![codecov](https://codecov.io/gh/pusher/chatkit-android/branch/master/graph/badge.svg)](https://codecov.io/gh/pusher/chatkit-android)
[![Travis branch](https://img.shields.io/travis/pusher/chatkit-android/master.svg)](https://travis-ci.org/pusher/chatkit-android)

The Android client for Pusher Chatkit. If you aren't already here, you can find the source on [Github](https://github.com/pusher/chatkit-android).

For more information on the Chatkit service, see [here](http://pusher.com/chatkit). For full documentation, see [here](https://docs.pusher.com/chatkit/overview/)

The SDK is written in Kotlin, but aimed to be as Java-friendly as possible.

## Index:

 * [Features](#Features)
 * [Setup](#Setup)
   * [Include it in project](#include-it-in-project)
 * [Usage](#usage)
   * [Instantiate Chatkit](#instantiate-chatkit)
   * [Connecting](#connecting)
   * [Chat Events](#chat-events)
   * [Termination](#termination)
 * [CurrentUser](#currentuser)
 * [Rooms](#rooms)
   * [Creating a room](#creating-a-room)
 * [Development Build](#development-build)
 

## Features

- Creating, joining, and deleting rooms
- Adding and removing users to rooms
- Sending and receiving messages to and from rooms, with attachments
- Seeing who's currently in a room
- Seeing who's currently online

## Setup

### Include it in project

You can install the SDK via Gradle. First add this to your $PROJECT_ROOT/app/build.gradle

```groovy
dependencies {
    // ...
    implementation 'com.pusher:chatkit-android:$chatkit-version'
}
```

## Usage

### Instantiate Chatkit

To get started with Chatkit you will need to instantiate both a `ChatManager` instance as well as a `TokenProvider` instance to authenticate users. The example below uses demo credentials.

Builder pattern

TokenProvider - testTokenProvider

We provide you with a sample token provider implementation. You can enable / disable it in the dashboard.
To include it in your application, create it with your details, as such:


```kotlin

const val INSTANCE_LOCATOR = "v1:us1:80215247-1df3-4956-8ba8-9744ffd12161"
const val TOKEN_PROVIDER_ENDPOINT = "your.auth.url"
const val USER_ID = "sarah"

val chatManager = ChatManager(
    instanceLocator = INSTANCE_LOCATOR, 
    userId = USER_ID,
    dependencies = AndroidChatkitDependencies(
        context = getApplicationContext(),
        tokenProvider = ChatkitTokenProvider(TOKEN_PROVIDER_ENDPOINT)
    )
)

```

This is how we do it on our demo app: [ChatkitDemoApp](https://github.com/pusher/android-slack-clone/blob/master/app/src/main/kotlin/com/pusher/chatkitdemo/ChatKitDemoApp.kt#L47)

 - `instanceLocator`: You can find this in the "Keys" section of our dashboard: https://dash.pusher.com/

 - `userId`: Used to identify the user that will be connected with this `ChatManager` instance.

 - `dependencies`: Contains some requirements needed for `ChatManager`. We provide a ready made type for `ChatkitDependencies` for android, so all you have to do is provide a `Context` and a `TokenProvider`. 

We also have available an implementation for `tokenProvider` which just needs the url to authorize users. If you have enabled testing on the `Settings` section of our dashboard, you can get a test url for this purpose in there. For production applications you have to create your own server side. More information about this can be found here: https://docs.pusher.com/chatkit/reference/server-node.

### Connecting

The simplest way to connect returns a `Future` which will provide either a `CurrentUser` or an `Error`. 

```kotlin
val futureUser: Future<Result<CurrentUser, Error>> = chatManager.connect()

```

You can observe the result from your favourite threading tool. We also provide a convenience extension that makes it more semantic to wait for the results of the future:

```kotlin
val userResult = futureUser.wait() // waits 10 seconds by default
//or
val userResult = futureUser.wait(For(30, SECONDS))

```

> Note: both `get()` and `wait()` will block the current thread so make sure that you are on a background thread.

To consume the result we can do this:

```kotlin
chatManager.connect().wait().let { result ->
    when(result) {
      is Result.Success -> toast("User received: ${result.value.name})")
      is Result.Failure -> toast("Oops: ${result.error})")
    }
  }
```

Alternatively, we have included a `fold` method too:

```kotlin
chatManager.connect().wait().fold(
  onSuccess = { user -> toast("User received: ${user.name})") },
  onFailure = { error -> toast("Oops: ${result.error})") }
)
```

If you are using coroutines this can be wrapped into a suspending method like this:

```kotlin
suspend fun ChatManager.connectForUser(): Result<CurrentUser, Error> = 
  suspendCoroutine{ c -> c.resume(connect().wait()) }
  
// or, if want to treat error as an exception:

suspend fun ChatManager.connectForUser(): CurrentUser = suspendCoroutine { c ->
  connect().wait().let { result ->
    when(result) {
      is Result.Success -> c.resume(result.value)
      is Result.Failure -> c.resumeWithException(RuntimeException(result.error.reason))
    }
  }
}
```

If you use `RxJava` you can wrap this inside a Single:

```kotlin
fun ChatManager.connectForUser(): Single<CurrentUser> = Single.create { emitter ->
  connect().wait().let { result ->
    when(result) {
      is Result.Success -> emitter.onSuccess(result.value)
      is Result.Failure -> emitter.onError(RuntimeException(result.error.reason))
    }
  }
}
```

#### Result

We've been referring this `Result` without any explanation. It it nothing more than a rename of the functional pattern called `Either`. It a bit like Schrodinger's cat it can either have a success or a failure. If you want to learn more about this we go into details [here](/docs/Result.md)

### Chat events

When connecting to `ChatManager` we can also register for global events.

If you only care about a number of events you can provide a `ChatManagerListeners` implementation with the events you want:

```kotlin

val user = chatManager.connect(ChatManagerListeners(
      onUserCameOnline = { user -> toast("${user.name} came online") },
      onUserWentOffline = { user -> toast("${user.name} went ofline") }
))

``` 

Alternatively you can listen to all events with a single listener:

```kotlin
val user = chatManager.connect { event ->
  when(event) {
    is UserCameOnline -> toast("${event.user.name} came online")
    is UserWentOffline -> toast("${event.user.name} went ofline")
  }
}
```

The available events are:

 | Event                      | Properties     | Description                                                       |
 |----------------------------|----------------|-------------------------------------------------------------------|
 | CurrentUserReceived        | CurrentUser    | Happens when the logged user is available or updated              |
 | UserStartedTyping          | User           | User has started typing                                           |
 | UserStoppedTyping          | User           | User has stopped typing                                           |
 | UserJoinedRoom             | User, Room     | User has joined the provided room                                 |
 | UserLeftRoom               | User, Room     | User has left the provided room                                   |
 | UserCameOnline             | User           | User is now online                                                |
 | UsersUpdated               | Nothing        | User is now offline                                               |
 | CurrentUserAddedToRoom     | Room           | Current user was added to a room                                  |
 | CurrentUserRemovedFromRoom | Int (room id)  | Current user was removed from a room with the given id            |
 | RoomUpdated                | Room           | Happens when the logged user is available or updated              |
 | RoomDeleted                | Int (room id)  | Happens when the logged user is available or updated              |
 | ErrorOccurred              | (Pusher)Error  | An error occurred, it does not mean the subscription has finished |
 
Each of the events have a relevant listener that can be set on `ChatManagerListeners`

### Termination

When you are done using the `ChatkitManager` you can call the `close` function which will try to terminate any pending requests and/or subscriptions.

```kotlin
chatManager.close()
```

## CurrentUser

When an initial connection is successfully made to Chatkit the client will receive a `CurrentUser` object. The `CurrentUser` object is the primary means of interacting with Chatkit.

 | Property         | Type                             | Description                                                            |
 |------------------|----------------------------------|------------------------------------------------------------------------|
 | rooms            | List<Room>                       | The rooms that the connected user is a member of.                      |
 | users            | Future<Result<List<User>, Error> | The users that share a common room membership with the connected user. |
 
The `users` property is a `Future` because it may not have all the required information for all the users so it must go get it, which in turn may fail.

## Rooms

There are a few important things to remember about Chatkit rooms; they are either public or private, users that are members of a room can change over time, all chat messages belong to a room.

 | Property      | Type           | Description                                                         |
 |---------------|----------------|---------------------------------------------------------------------|
 | id            | Int            | The global identifier for the room on the instance.                 |
 | createdById   | Int            | The id of the user that created this room                           |
 | name          | String         | The human readable name of the room (this needn’t be unique!)       |
 | memberUserIds | Set<String>    | A set of ids for everyone on the room                               |
 | isPrivate     | Boolean        | If true the room is private, otherwise the room is public.          |

### Creating a room

All that you need to provide when creating a room is a name. The user that creates the room will automatically be added as a member of the room.

The following code will create a public room called `"my room name"`. Note that a room name must be no longer than 60 characters.

```kotlin
val newRoom: Future<Result<Room, Error>> = currentUser.createRoom("my room name")
```

Same as before, the result can be consumed (inside a background thread) like:

``` kotlin
currentUser.createRoom("my room name").wait().fold(
  onSuccess = { room -> toast("Hurra! room created: ${room.name})") },
  onFailure = { error -> toast("Oops: ${result.error})") }
)
```

If you want to make a private room ir can be done:

```kotlin
currentUser.createRoom(
  name = "my room name",
  private = true  
)
```

Also, you may choose to provide an initial number of users to be part of that room (i.e. one-to-one conversations), in which case you can also provide it with a list of users:

```kotlin
currentUser.createRoom(
  name = "my room name",
  userIds = listOf("sarah", "pusherino")
)
```




### Development build

When building this project, you may choose to use a local version of [`pusher-platform-android`](1). 

To do so you can add the path to your local copy in your `~/.gradle/gradle.properties`:

````
pusher_platform_local=../pusher-platform-android
````

It can be either a relative or absolute path.

[1]: https://github.com/pusher/pusher-platform-android

