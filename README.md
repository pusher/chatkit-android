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
   * [Token provider](#token-provider)
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
   * [Cancel a subscription](#cancel-a-subscription)
 * [Users](#users)
 * [Messages](#messages)
   * [Sending a message](#sending-a-message)
 * [Attachment](#attachment)
   * [Fetching an attachment](#fetching-an-attachment)
 * [Typing indicators](#typing-indicators)
   * [Trigger a typing event](#trigger-a-typing-event)
   * [Receive typing indicators](#receive-typing-indicators)
 * [User presence](#user-presence)
 * [Cursors](#cursors)
   * [Setting a cursor](#setting-a-cursor)
   * [Getting a cursor](#getting-a-cursor)
   * [Access other user's cursor](#access-other-user's-cursors)
 * [Logger](#logger)
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
        tokenProvider = ChatkitTokenProvider(TOKEN_PROVIDER_ENDPOINT, USER_ID)
    )
)

```

This is how we do it on our demo app: [ChatkitDemoApp](https://github.com/pusher/android-slack-clone/blob/master/app/src/main/kotlin/com/pusher/chatkitdemo/ChatKitDemoApp.kt#L47)

 - `instanceLocator`: You can find this in the "Keys" section of our dashboard: https://dash.pusher.com/

 - `userId`: Used to identify the user that will be connected with this `ChatManager` instance.

 - `dependencies`: Contains some requirements needed for `ChatManager`. We provide a ready made type for `ChatkitDependencies` for android, so all you have to do is provide a `Context` and a `TokenProvider`. 

We also have available an implementation for `tokenProvider` which just needs the url to authorize users. If you have enabled testing on the `Settings` section of our dashboard, you can get a test url for this purpose in there. For production applications you have to create your own server side. More information about this can be found here: https://docs.pusher.com/chatkit/reference/server-node.

## Token provider

Although we provide a version of the `TokenProvider` that works with a url to a remove token provider (`ChatkitTokenProvider`), it is possible to create a custom one. These are the functions required by the `TokenProvider` interface:

 | Function   | Params                      | Return                        | Description                                                               |
 |------------|-----------------------------|-------------------------------|---------------------------------------------------------------------------|
 | fetchToken | tokenParams (`Any, `Object`)| Future<Result<String, Error>> | Provides a string with the token or an error if failed (it can be cached) |
 | clearToken | String                      | `Unit`, void                  | Called when chatkit requires a fresh token                                |

The implementation of `ChatkitTokenProvider` has the following properties:


 | Property   | Type                           | Description                                                          |
 |------------|--------------------------------|----------------------------------------------------------------------|
 | endpoint   | String                         | Url for the server that provides access tokens                       |
 | userId     | String                         | Name of the user login in                                            |
 | authData   | Map<String, String> (Optional) | `CustomData` sent to the server along with `TokenParams`             |
 | client     | OkHttpClient (Optional)        | Used for networking (i.e. can modify to use proxy)                   |
 | tokenCache | TokenCache (Optional)          | By default we use an in memory but can provide a custom `TokenCache` |

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
 | UserJoinedRoom             | User, Room     | User has joined the provided room                                 |
 | UserLeftRoom               | User, Room     | User has left the provided room                                   |
 | UserCameOnline             | User           | User is now online                                                |
 | UsersUpdated               | Nothing        | User is now offline                                               |
 | CurrentUserAddedToRoom     | Room           | Current user was added to a room                                  |
 | CurrentUserRemovedFromRoom | Int (room id)  | Current user was removed from a room with the given id            |
 | RoomUpdated                | Room           | Happens when the logged user is available or updated              |
 | RoomDeleted                | Int (room id)  | Happens when the logged user is available or updated              |
 | NewReadCursor              | Int (room id)  | Happens when a new cursor is set for `CurrentUser`                |
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
  ),
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
 | UserJoined        | Int (userId) | User has joined the room                              |
 | UserLeft          | Int (userId) | User has left the room                                |
 | UserCameOnline    | User         | User is now online                                    |
 | UserWentOffline   | User         | User is now offline                                   |
 | NewReadCursor     | Cursor       | A member of the room set a new read cursor.           |
 
Each of the events have a relevant listener that can be set on `RoomSubscriptionListeners`
 
### Cancel a subscription

The `subscribeToRoom` function returns a `Subscription` that can be cancelled by calling `subscription.unsubscribe()` when the subscription is no longer needed.

Alternatively, it is possible to close all active subscriptions by calling `chatManager.cancel()`, which will close all these subscriptions.

## Users

User objects can be found in various places: globally under `currentUser.users` or returned as the argument to some callbacks.

### User properties

 | Property  | Type     | Description                                                                             |
 |-----------|----------|-----------------------------------------------------------------------------------------|
 | id        | String   | The unique identifier for the user on the instance.                                     |
 | name      | String   | The human readable name of the user. This is not required to be unique.                 |
 | avatarUrl | String   | The location (url) of an avatar for the user.                                           |
 | presence  | Presence | An object containing information regarding the users presence state. See user presence. |
 
Rooms contain a list of user ids, to resolve these you can use this:

```kotlin
currentUser.usersforRoom(someRoom)
```
 
## Messages
 
Every message belongs to a [Room](#rooms) and has an associated sender, which is represented by a [User](#users) object. Files can be sent along with a messages by specifying an [Attachment](#attachment) property.
 
### Message properties

 | Property   | Type       | Description                                            |
 |------------|------------|--------------------------------------------------------|
 | id         | Int        | The Id assigned to the message by the Chatkit servers. |
 | text       | String     | The text content of the message if present.            |
 | attachment | Attachment | The message’s attachment if present.                   |                      
 | sender     | User       | The user who sent the message.                         |
 | room       | Room       | The room to which the message belongs.                 |
 | createdAt  | String     | The timestamp at which the message was created.        |
 | updatedAt  | String     | The timestamp at which the message was last updated.   |

### Sending a message

To send a message:

```kotlin
currentUser.sendMessage(
  room = someRoom, // also available as roomId: Int
  messageTest = "Hi there! 👋",
  attachment = NoAttachment // Optional, NoAttachment by default
).wait().let { result -> 
  when(result) { // Result<Int, Error>, either the new message id or an error
    is Result.Success -> toast("CurrentUser left room: ${result.value.name}.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```
An attachment can be added when you send a message. This can be done in one of two ways:

1. Provide some data (of type [File], most likely) along with a name for the data that will be used as the name of the file that is stored by the Chatkit servers.

This is how you send a message with an attachment of this kind:

```kotlin
currentUser.sendMessage(
  room = someRoom,
  messageTest = "Hi there! 👋",
  attachment = DataAttachment(
    file = File("file/path.jpg"),
    name = "file-name" // optional, "file" by default
  )
).wait().let { result -> 
  when(result) { // Result<Int, Error>, either the new message id or an error
    is Result.Success -> toast("CurrentUser left room: ${result.value.name}.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

Note that the resulting type will be inferred automatically by Chatkit servers. If the type of the file is unable to be determined then it will be given a type of `file`.

2. Provide a link along with a type that describes the attachment. As above, this would be one of `image`, `video`, `audio`, or `file`.

This is how you send a message with an attachment of this kind:

```kotlin
currentUser.sendMessage(
  room = someRoom,
  messageTest = "Hi there! 👋",
  attachment = LinkAttachment(
    link = File("file/path.jpg"),
    type = AttachmentType.IMAGE
  )
).wait().let { result -> 
  when(result) { // Result<Int, Error>, either the new message id or an error
    is Result.Success -> toast("CurrentUser left room: ${result.value.name}.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

## Attachment

It is possible for users to attach files to messages. If a message has an attachment you will most likely have to fetch it before you can use it. This will give you the actual URL of the resource.

### Attachment properties

 | Property      | Type            | Description                                                                                   |
 |---------------|-----------------|-----------------------------------------------------------------------------------------------|
 | link          | String          | The link representing the location of the attachment.                                         |
 | type          | AttachmentType  | The type of the attachment; one of image, video, audio, or file.                              |
 | fetchRequired | Boolean         | If the attachment link needs to be fetched from the Chatkit servers; see Fetch an Attachment. |

### Fetching an attachment

If a message contains an attachment with the `fetchRequired` property set to `true`, then `attachment.link` cannot be used directly. We must first fetch the URL of the attachment itself using `fetchAttachment`.

```kotlin
currentUser.fetchAttachment(
  attachmentUrl = message.link
).wait().let { result -> // Future<Result<FetchedAttachment, Error>>
  when(result) { // Result<Int, Error>, either the new message id or an error
    is Result.Success -> toast("Loaded attachment: ${result.value.link}.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

## Typing indicators

Sometimes it’s useful to be able to see if another user is typing. You can use Chatkit to let all the connected members of a room know when another user is typing.

### Trigger a typing event

To send typing indicator events call `startedTypingIn` with the id of the room the current user is typing in.

```kotlin
currentUser.startedTypingIn(
  roomId = room.id
).wait().let { result -> // Future<Result<Unit, Error>>
   when(result) { // Result<Int, Error>, either the new message id or an error
     is Result.Success -> toast("Success!")
     is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
   }
 }
```

### Receive typing indicators

To be notified when a user starts or stops typing in a room, provide a `onUserStartedTyping` function as part of the room subscription listener.

```kotlin
RoomSubscription(
  onUserStartedTyping = { user -> toast("User ${user.name} started typing") }
)
```

Alternatively, if you are using an event callback:

```kotlin
{ event -> 
  when(event) {
    is UserStartedTyping -> toast("User ${event.user.name} started typing")
  } 
}
```

## User presence

If a user has at least one active connection to the Chatkit service then they are considered online. When a user has no active connections they are considered offline. Each [user object](#users) keeps track of whether a user is online or offline via the presence property.

```kotlin
if(user.presence is User.Presence.Online) {
  // The user is online! Show an online badge or something...
}
```

Additionally, to be notified when a user comes online or goes offline, you can provide the `onUserCameOnline` and `onUserWentOffline` listeners or match the `UserCameOnline` and `UserWentOffline` events. Either at the [room level](#room-subscription-events) – fires whenever a member of that room goes on or off line, or at the [connection level](#connecting) – fires whenever any users sharing a common room membership go on or offline.

```kotlin
chatManager.connect { event ->
  when(event) {
    is UserCameOnline -> toast("User ${event.user.name} came online.")
    is UserVentOffline -> toast("User ${event.user.name} went offline.")
  } 
}
```

## Cursors

Read cursors track how far a user has read through the messages in a room. Each read cursor belongs to a user and a room – represented by a `Cursor` object.

### Cursor properties


 | Property  | Type             | Description                                                                     |
 |-----------|------------------|---------------------------------------------------------------------------------|
 | position  | String           | The [message](#messages) ID that the user has read up to.                       |
 | updatedAt | String           | The timestamp when the cursor was last set.                                     |
 | room      | Int (room id)    | The [room](#rooms) that the cursor refers to.                                   |
 | user      | String (user id) | The [user](#users) that the cursor belongs to.                                  |
 | type      | Int              | The type of the cursor object, currently always 0 (representing a read cursor). |

### Setting a cursor

When you are confident that the current user has “read” a message, call `setReadCursor` with a `roomId` and a `position` (the id of the newest message that has been “read”).

```kotlin
currentUser.setReadCursor(
  roomId = someRoomId,
  position = someMessageId
).wait().let { result -> // Future<Result<Boolean, Error>>
  when(result) { // Result<Int, Error>, either the new message id or an error
    is Result.Success -> toast("Cursor set!")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

### Getting a cursor

The current user’s read cursors are available immediately upon connecting. Access any existing cursors with the `readCursor` function. (A cursor that hasn’t been set yet is undefined.)

```kotlin
currentUser.readCursor(
  roomId: romroomId
)
```

**Note:** To be notified when any of the current user’s read cursors are changed, supply an `onNewReadCursor` listener on connection or match for `NewReadCursor` events.

### Access other user's cursors

After subscribing to a room, read cursors for members of that room can be accessed by supplying a `userId` as the second parameter to the `readCursor` method.

```kotlin
currentUser.readCursor(
  roomId: romroomId,
  userId: "alice"
).wait().let { result -> // Future<Result<Boolean, Error>>
  when(result) { // Result<Int, Error>, either the new message id or an error
    is Result.Success -> toast("Cursor set!")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

To be notified when any member of the room changes their read cursor, supply an `onNewReadCursor` listener [when subscribing to the room](#room-subscription-events) or match the `NewReadCursor` event.

## Logger

As part of `ChatManager` dependencies a custom logger can be provided:

```kotlin
val chatManager = ChatManager(
    instanceLocator = INSTANCE_LOCATOR, 
    userId = USER_ID,
    dependencies = AndroidChatkitDependencies(
        context = getApplicationContext(),
        tokenProvider = ChatkitTokenProvider(TOKEN_PROVIDER_ENDPOINT),
        logger = object : Logger {
          fun verbose(message: String, error: Error? = null) = println("V: $message")
          fun debug(message: String, error: Error? = null) = println("D: $message")
          fun info(message: String, error: Error? = null) = println("I: $message")
          fun warn(message: String, error: Error? = null) = println("W: $message")
          fun error(message: String, error: Error? = null) = println("E: $message")
        }
    )
)
```

## Development build

When building this project, you may choose to use a local version of [`pusher-platform-android`](1). 

To do so you can add the path to your local copy in your `~/.gradle/gradle.properties`:

````
pusher_platform_local=../pusher-platform-android
````

It can be either a relative or absolute path.

[1]: https://github.com/pusher/pusher-platform-android

