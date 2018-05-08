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
   * [Fetching messages for a Room](#fetching-messages-for-a-room)
   * [Add User to a Room](#add-user-to-a-room)
   * [Remove user from a Room](#remove-user-from-a-room)
   * [Get joinable Rooms](#get-joinable-rooms)
   * [Joining a Room](#joining-a-room)
   * [Leaving a Room](#leaving-a-room)
   * [Update a Room](#update-a-room)
   * [Delete a Room](#delete-a-room)
 * [Subscriptions](#subscriptions)
   * [Room subscription events](#room-subscription-events)
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
    when(result) { // Result<CurrentUser, Error>
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
    when(result) { // Result<CurrentUser, Error>
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
    when(result) { // Result<CurrentUser, Error>
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

### Fetching messages for a Room

You can fetch up to the last 100 messages added to a room when you subscribe (Using `messageLimit`) but sometimes you’ll want to fetch older messages. For example, suppose you subscribe to a room and the oldest message you see has the ID 42. To see older messages, you can provide the initialId option to the fetchMessages method.

```kotlin
currentUser.fetchMessages(
  room = someRoom,
  initialId = 42, // Optional
  direction = NEWER_FIRST, // Optional, OLDER_FIRST by default
  limit = 20 // Optional, 10 by default
).wait().let { result -> 
  when(result) { // Result<List<Message>, Error>
    is Result.Success -> toast("Mesages ${result.value} received.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

Instead of a room instance it is also possible to fetch messages using the room id.

```kotlin
currentUser.fetchMessages(roomId = 123)
```

The full set of options follows:

 | Property    | Type                 | Description                                                                        |
 |-------------|----------------------|------------------------------------------------------------------------------------|
 | initialId   | Int (Optional)       | A message ID that defaults to the most recent message ID.                          |
 | direction   | Direction (Optional) | Defaults to `OLDER_FIRST`, dictates the direction of the messages being returned.  |
 | limit       | Int (Optional)       | Limits the number of messages that we get back, defaults to 10.                    |
 
 
### Add User to a Room

The current user can add users to rooms that they themselves are a member of.

```kotlin
currentUser.addUsersToRoom(
  userIds = listOf("keith"),
  room = someRoom
).wait().let { result -> 
   when(result) { // Result<Unit, Error>
     is Result.Success -> toast("Successfully added users.")
     is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
   }
 }
```

### Remove user from a Room

The current user can remove users from rooms that they themselves are a member of.

```kotlin
currentUser.removeUsersFromRoom(
  userIds = listOf("keith"),
  room = someRoom
).wait().let { result -> 
   when(result) { // Result<Unit, Error>
     is Result.Success -> toast("Successfully removed users.")
     is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
   }
 }
```

### Get joinable Rooms

To fetch a list of the rooms that a user is able to join (but isn’t yet a member of):


```kotlin
currentUser.getJoinableRooms().wait().let { result -> 
 when(result) {  // Result<List<Room>, Error>
   is Result.Success -> toast("The user can join ${result.value}.")
   is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
 }
}
```

The rooms returned will be a list of the public rooms which the `currentUser` is not a member of.

### Joining a Room

Join a room with ID `someRoomId`:

```kotlin
currentUser.joinRoom(
  roomId = someRoomId
).wait().let { result -> 
  when(result) { // Result<Room, Error>
    is Result.Success -> toast("CurrentUser joined room: ${result.value.name}.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
 }
```

### Leaving a Room

Leave a room with ID `someRoomId`:

```kotlin
currentUser.leaveRoom(
  roomId = someRoomId
).wait().let { result -> 
  when(result) { // Result<Int, Error>
    is Result.Success -> toast("CurrentUser left room: ${result.value.name}.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
 }
```


### Update a Room

Change the name and or privacy of a room with Id `someRoomId`:

```kotlin
currentUser.updateRoom(
  roomId = someRoomId,
  name = "Some updated name",
  private = false // Optional
).let { result -> 
   when(result) { // Result<Unit, Error>
     is Result.Success -> toast("Updated room.")
     is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
   }
  }
```

All other connected members of the room will [receive an event](#chat-events) that informs them that the room has been updated. Note that the current user must have the `room:update` [permission](https://docs.pusher.com/chatkit/reference/roles-and-permissions) to use this method.

Note: This only returns whether the action is successful. To get the new room we have to handle the event that we get or fetch a new room.

### Delete a Room

Delete a room with ID `someRoomId`:

```kotlin
currentUser.deleteRoom(
  roomId = someRoomId
).let { result -> 
    when(result) { // Result<Unit, Error>
      is Result.Success -> toast("Updated room.")
      is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
    }
   }
```

All other connected members of the room will [receive an event](#chat-events) that informs them that the room has been deleted. Any attempts to interact with a deleted room will result in an error. Note that the current user must have the `room:delete` [permission](https://docs.pusher.com/chatkit/reference/roles-and-permissions) to use this method.

**Note:** Deleting a room will delete all the associated messages too.

## Subscriptions

To be notified when new messages are added to a room, you’ll need to subscribe to it and provide a `RoomSubscriptionListeners` instance or a lambda to listen for `RoomSubscriptionEvent`. (Too see the full list of possible hooks see [Room Subscription Hooks](#room-subscription-hooks)). At most 100 recent messages can be retrieved on subscription, to fetch older messages see [Fetching Messages From a Room](#fetching-messages-for-a-room). To receive only new messages, set the `messageLimit` to 0.

Using `RoomSubscriptionListeners`:

```kotlin
currentUser.subscribeToRoom(
  roomId = someroomId,
  listeners = Roomsubscription(
    onNewMesage = { message -> toast("${message.userId} says: ${message.text}") },
    onErrorOccurred = { error -> toast("Oops something bad happened: $error") }
  )
  messageLimit = 10 // Optional, 10 by default
)
```

Using `RoomSubscriptionEvent`:

```kotlin
currentUser.subscribeToRoom(
    roomId = someroomId,
    messageLimit = 10 // Optional, 10 by default
) { event ->
  when(event) {
    is NewMessage -> toast("${event.message.userId} says: ${event.message.text}")
    is ErrorOccurred -> toast("Oops something bad happened: ${event.error}")
  }
}
```

**Note:** Subscribing implicitly joins a room if you aren’t already a member. Subscribing to the same room twice will cause the existing subscription to be cancelled and replaced by the new one.

By default when you subscribe to a room you will receive up to the 10 most recent messages that have been added to the room. The number of recent messages to fetch can be configured by setting the `messageLimit` parameter`. These recent messages will be passed to the `onNewMessage` callback (or as `NewMessage` event) in the order they were sent, just as if they were being sent for the first time.

### Room subscription events

This is the full list of available events from a room subscription:

 | Event             | Properties   | Description                                           |
 |-------------------|--------------|-------------------------------------------------------|
 | NewMessage        | Message      | A new message has been added to the room.             |
 | UserStartedTyping | User         | User has started typing                               |
 | UserStoppedTyping | User         | User has stopped typing                               |
 | UserJoined        | Int (userId) | User has joined the room                              |
 | UserLeft          | Int (userId) | User has left the room                                |
 | UserCameOnline    | User         | User is now online                                    |
 | UserWentOffline   | User         | User is now offline                                   |
 | NewReadCursor     | Cursor       | A member of the room set a new read cursor.           |
 
Each of the events have a relevant listener that can be set on `RoomSubscriptionListeners`
 
### Cancel Subscription

The `subscribeToRoom` function returns a `Subscription` that can be cancelled by calling `subscription.unsubscribe()` when the subscription is no longer needed.

Alternatively, it is possible to close all active subscriptions by calling `chatManager.cancel()`, which will close all these subscriptions.

## Users

## Development build

When building this project, you may choose to use a local version of [`pusher-platform-android`](1). 

To do so you can add the path to your local copy in your `~/.gradle/gradle.properties`:

````
pusher_platform_local=../pusher-platform-android
````

It can be either a relative or absolute path.

[1]: https://github.com/pusher/pusher-platform-android

