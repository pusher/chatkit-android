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
 * [Publishing](#publishing)
   * [jCenter](#jcenter)
   * [Maven](#maven)


## Features

- Creating, joining, and deleting rooms
- Adding and removing users to rooms
- Sending and receiving messages to and from rooms, with attachments
- Seeing who's currently in a room
- Seeing who's currently online

## Setup

### Include it in project

You can install the SDK via Gradle. First add this to your
`$PROJECT_ROOT/app/build.gradle`

```groovy
dependencies {
    // ...
    implementation 'com.pusher:chatkit-android:$chatkit-version'
}
```

## Usage

### Instantiate Chatkit

To get started with Chatkit you will need to instantiate both a `ChatManager`
instance as well as a `TokenProvider` instance to authenticate users. The
example below uses demo credentials.

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

 - `instanceLocator`: You can find this in the "Keys" section of our dashboard:
   https://dash.pusher.com/
 - `userId`: Used to identify the user that will be connected with this
   `ChatManager` instance.
 - `dependencies`: Contains some requirements needed for `ChatManager`. We
   provide a ready made type for `ChatkitDependencies` for Android, so all you
   have to do is provide a `Context` and a `TokenProvider`.

We also have available an implementation for `tokenProvider` which just needs
the URL to authorize users. If you have enabled the test token provider on the
`Settings` section of our dashboard, you can get a test URL for this purpose
from there.

For production applications you should create your own server
side.  More information about this can be found here:
https://docs.pusher.com/chatkit/reference/server-node.

## Token provider

Although we provide a version of the `TokenProvider` that works with a url to a
remote token provider (`ChatkitTokenProvider`), it is possible to create a
custom one. These are the functions required by the `TokenProvider` interface:

 | Function   | Params                      | Return                        | Description                                                               |
 |------------|-----------------------------|-------------------------------|---------------------------------------------------------------------------|
 | fetchToken | tokenParams (`Any, `Object`)| Result<String, Error>         | Provides a string with the token or an error if failed (it can be cached) |
 | clearToken | String                      | `Unit`, void                  | Called when chatkit requires a fresh token                                |

The implementation of `ChatkitTokenProvider` has the following properties:

 | Property   | Type                           | Description                                                          |
 |------------|--------------------------------|----------------------------------------------------------------------|
 | endpoint   | String                         | Url for the server that provides access tokens                       |
 | userId     | String                         | Name of the user login in                                            |
 | authData   | Map<String, String> (Optional) | `CustomData` sent to the server                                      |
 | client     | OkHttpClient (Optional)        | Used for networking (i.e. can modify to use proxy)                   |
 | tokenCache | TokenCache (Optional)          | By default we use an in memory but can provide a custom `TokenCache` |

### Connecting

The simplest way to connect returns a `Result` of either a `CurrentUser` or an
`Error`. It will block until connections to the backend are complete and
initial state has been received.

```kotlin
val user: Result<CurrentUser, Error> = chatManager.connect()
```

All calls in the public API which have the potential to fail return a `Result` type rather than throwing exceptions on failure. There are a couple of ways to unwrap a result:

#### Pattern matching

`Result`s are a union type consisting of either a `Success` or `Failure`:

```kotlin
chatManager.connect().let { result -> // : Result<CurrentUser, Error>
  when (result) {
    is Result.Success -> toast("User received: ${result.value.name})")
    is Result.Failure -> toast("Oops: ${result.error})")
  }
}
```

#### Convert to exception

If you prefer failures to raise exceptions, you can call `successOrThrow()`
which either returns the unwrapped success value or throws the error as an
exception:

```kotlin
try {
  val user = chatManager.connect().successOrThrow()
  toast("User received: ${user.name})")
}
catch e: elements.Error {
  toast("Oops: ${e}")
}
```

#### Other ways

The `Result` type is inspired by the functional programming community. Several
other useful methods for working in a functional style are implemented,
including `map` and `fold`. If you want to learn more about this we go into
details [here](/docs/Result.md), but the two approaches above are both
sufficient for getting full use of the library.

### Chat events

When connecting the `ChatManager` we can register handlers for events.

You can provide distinct handler functions for each event type by passing a `ChatListeners` implementation:

```kotlin
val userResult = chatManager.connect(ChatListeners(
    onUserCameOnline = { user -> toast("${user.name} came online") },
    onUserWentOffline = { user -> toast("${user.name} went offline") },
    onRoomUpdated = { room -> toast("${room.name} had its properties updated") }
))

```

Alternatively you can pass a function `(ChatEvent) -> Unit` which will be
invoked for every event, and then pattern match on the event types:

```kotlin
val userResult = chatManager.connect { event ->
  when (event) {
    is UserCameOnline -> toast("${event.user.name} came online")
    is UserWentOffline -> toast("${event.user.name} went offline")
    is RoomUpdated -> toast("${event.room.name} had its properties updated")
    else -> toast("Got to be exhaustive. Some other event arrived")
  }
}
```

The available events are:

 | Event                      | Properties       | Description                                                              |
 |----------------------------|------------------|--------------------------------------------------------------------------|
 | CurrentUserReceived        | CurrentUser      | Happens when the logged in user is available and the SDK is ready to use |
 | UserStartedTyping          | User, Room       | User has started typing                                                  |
 | UserStoppedTyping          | User, Room       | User has stopped typing                                                  |
 | UserJoinedRoom             | User, Room       | User has joined the provided room                                        |
 | UserLeftRoom               | User, Room       | User has left the provided room                                          |
 | UserCameOnline             | User             | User is now online                                                       |
 | UserWentOffline            | String (user id) | User is now offline                                                      |
 | CurrentUserAddedToRoom     | Room             | Current user was added to a room                                         |
 | CurrentUserRemovedFromRoom | Int (room id)    | Current user was removed from a room with the given id                   |
 | RoomUpdated                | Room             | Happens when the logged user is available or updated                     |
 | RoomDeleted                | Int (room id)    | Happens when the logged user is available or updated                     |
 | NewReadCursor              | Int (room id)    | Happens when a new cursor is set for `CurrentUser`                       |
 | ErrorOccurred              | (Pusher)Error    | An error occurred, it does not mean the subscription has finished        |

Each of the events have a relevant listener that can be set on `ChatListeners`

### Termination

When you are done using the `ChatkitManager` you can call the `close` function
which will terminate any pending requests and/or subscriptions.

```kotlin
chatManager.close()
```

## CurrentUser

When an initial connection is successfully made to Chatkit the client will
receive a `CurrentUser` object. The `CurrentUser` object is the primary means
of interacting with Chatkit.

 | Property         | Type                     | Description                                                            |
 |------------------|--------------------------|------------------------------------------------------------------------|
 | rooms            | List<Room>               | The rooms that the connected user is a member of.                      |
 | users            | Result<List<User>, Error | The users that share a common room membership with the connected user. |

The `users` property is a `Result` because it may have to make a request to the
server to fill in all properties of the users, and that request might fail.

## Rooms

There are a few important things to remember about Chatkit rooms:

- they are either public or private
- users that are members of a room can change over time
- all chat messages belong to a room

 | Property      | Type           | Description                                                         |
 |---------------|----------------|---------------------------------------------------------------------|
 | id            | Int            | The global identifier for the room on the instance.                 |
 | createdById   | Int            | The id of the user that created this room                           |
 | name          | String         | The human readable name of the room (this needn’t be unique!)       |
 | memberUserIds | Set<String>    | A set of ids for everyone on the room                               |
 | isPrivate     | Boolean        | If true the room is private, otherwise the room is public.          |

### Creating a room

All that you need to provide when creating a room is a name. The user that
creates the room will automatically be added as a member of the room.

The following code will create a public room called `my room name`.

Note that:

- a room name must be no longer than 60 characters
- room names are not unique

```kotlin
val newRoom: Result<Room, Error>> = currentUser.createRoom("my room name")
```

The returned `Room` object has an `id` property which is used in future to
interact with the room.

#### Private rooms

```kotlin
val newRoom = currentUser.createRoom(
  name = "my room name",
  private = true
)
```

#### Initial member list

The user creating a room is always added as the first member. If you know in
advance which other users should be in the room, then you can pass their IDs
and they will also be added at creation.

```kotlin
val newRoom = currentUser.createRoom(
  name = "my room name",
  userIds = listOf("sarah", "pusherino")
)
```

### Subscribing to a room

In order to receive new messages published to a room, you must subscribe.

Similarly to the ChatManager, you can consume `RoomEvent`s either by providing
a `RoomListener` object with functions per event, or a single function of
`RoomEvent -> Unit`s either by providing a `RoomListener` object with functions
per event, or a single function of `(RoomEvent) -> Unit`.

Using `RoomListeners`:

```kotlin
currentUser.subscribeToRoom(
  roomId = someroomId,
  listeners = RoomListeners(
    onMessage = { message -> toast("${message.userId} said: ${message.text}") },
    onErrorOccurred = { error -> toast("Oops something bad happened: $error") }
  ),
  messageLimit = 10 // Optional, 10 by default
)
```

Using `RoomEvent`:

```kotlin
currentUser.subscribeToRoom(
    roomId = someroomId,
    messageLimit = 10 // Optional, 10 by default
) { event ->
  when (event) {
    is Message -> toast("${event.message.userId} said: ${event.message.text}")
    is ErrorOccurred -> toast("Oops something bad happened: ${event.error}")
  }
}
```

**Note:** Subscribing implicitly joins a room if you aren’t already a member.
Subscribing to the same room twice will cause the existing subscription to be
cancelled and replaced by the new one.

By default when you subscribe to a room you will receive up to the 10 most
recent messages that have been added to the room. The number of recent messages
to fetch can be configured by setting the `messageLimit` parameter. These
recent messages will be passed to the `onMessage` callback (or as `Message`
event) in the order they were sent, just as if they were being sent for the
first time.

#### Room events

This is the full list of available events from a room subscription. You will
notice some overlaps with the `ChatEvent` type, though when consumed a room
scope, these events lack a `room` property, which is implicity the room which
the handler is registered for.

 | Event             | Properties   | Description                                           |
 |-------------------|--------------|-------------------------------------------------------|
 | Message           | Message      | A new message has been added to the room.             |
 | UserStartedTyping | User         | User has started typing                               |
 | UserStoppedTyping | User         | User has stopped typing                               |
 | UserJoined        | Int (userId) | User has joined the room                      TODO    |
 | UserLeft          | Int (userId) | User has left the room                                |
 | UserCameOnline    | User         | User is now online                                    |
 | UserWentOffline   | User         | User is now offline                                   |
 | NewReadCursor     | Cursor       | A member of the room set a new read cursor.           |

#### Cancel a subscription

The `subscribeToRoom` function returns a `Subscription` that can be closed by
calling `subscription.unsubscribe()` when the subscription is no longer needed.

Alternatively, it is possible to close all subscriptions by calling
`chatManager.close()`.

### Fetching messages for a Room

You can fetch up to the last 100 messages added to a room when you subscribe
(Using `messageLimit`) but sometimes you’ll want to fetch older messages. For
example, suppose you subscribe to a room and the oldest message you see has the
ID 42. To see older messages, you can provide the initialId option to the
fetchMessages method.

```kotlin
val olderMessages = currentUser.fetchMessages(
  room = someRoom,
  initialId = 42,          // Optional
  direction = NEWER_FIRST, // Optional, OLDER_FIRST by default
  limit = 20               // Optional, 10 by default
)

when (olderMessages) {
  is Result.Success -> toast("Messages ${result.value} received.")
  is Result.Failure -> toast("Oops, couldn't fetch messages: ${result.error}")
}
```

Instead of a room instance it is also possible to fetch messages using the room
id.

```kotlin
val olderMessages = currentUser.fetchMessages(roomId = 123)
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
).let { result ->
  when (result) { // Result<Unit, Error>
    is Result.Success -> toast("Successfully added users.")
    is Result.Failure -> toast("Oops, couldn't add user to room: ${result.error}")
  }
}
```

### Remove user from a Room

The current user can remove users from rooms that they themselves are a member of.

```kotlin
currentUser.removeUsersFromRoom(
  userIds = listOf("keith"),
  room = someRoom
).let { result ->
  when (result) { // Result<Unit, Error>
    is Result.Success -> toast("Successfully removed users.")
    is Result.Failure -> toast("Oops, couldn't remove user from room: ${result.error}")
  }
}
```

### Get joinable Rooms

To fetch a list of the rooms that a user is able to join (but isn’t yet a member of):

```kotlin
currentUser.getJoinableRooms().let { result ->
  when (result) {  // Result<List<Room>, Error>
   is Result.Success -> toast("The user can join ${result.value}.")
   is Result.Failure -> toast("Oops, failed to fetch room list: ${result.error}")
  }
}
```

The rooms returned will be a list of the public rooms which the `currentUser`
is not a member of.

### Joining a Room

Join a room with ID `someRoomId`:

```kotlin
currentUser.joinRoom(
  roomId = someRoomId
).let { result ->
  when (result) { // Result<Room, Error>
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
).let { result ->
  when (result) { // Result<Int, Error>
    is Result.Success -> toast("CurrentUser left room: ${result.value.name}.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

### Update a Room

Change the name and/or privacy of a room with ID `someRoomId`:

```kotlin
currentUser.updateRoom(
  roomId = someRoomId,
  name = "Some updated name",
  private = false // Optional
).let { result ->
  when (result) { // Result<Unit, Error>
    is Result.Success -> toast("Updated room.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

All other connected members of the room will [receive an event](#chat-events)
that informs them that the room has been updated. Note that the current user
must have the `room:update`
[permission](https://docs.pusher.com/chatkit/reference/roles-and-permissions)
to use this method.

Note: This only returns whether the action is successful. To get the new room
we have to handle the event that we get or fetch a new room.

### Delete a Room

Delete a room with ID `someRoomId`:

```kotlin
currentUser.deleteRoom(
  roomId = someRoomId
).let { result ->
  when (result) { // Result<Unit, Error>
    is Result.Success -> toast("Updated room.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

All other connected members of the room will [receive an event](#chat-events)
that informs them that the room has been deleted. Any attempts to interact with
a deleted room will result in an error. Note that the current user must have
the `room:delete`
[permission](https://docs.pusher.com/chatkit/reference/roles-and-permissions)
to use this method.

**Note:** Deleting a room will delete all the associated messages too.

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

Every message belongs to a [Room](#rooms) and has an associated sender, which
is represented by a [User](#users) object. Files can be sent along with a
messages by specifying an [Attachment](#attachment) property.

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
).let { result ->
  when (result) { // Result<Int, Error>, either the new message id or an error
    is Result.Success -> toast("CurrentUser left room: ${result.value.name}.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

#### Attachments

An attachment can be added when you send a message. This can be done in one of
two ways:

1. Provide some data (of type [File], most likely) along with a name for the
   data that will be used as the name of the file that is stored by the Chatkit
   server.

2. Provide a link along with a type that describes the attachment.
   This would be one of `image`, `video`, `audio`, or `file`. In this case the
   attachment data is not stored by Chatkit's server.


```kotlin
currentUser.sendMessage(
  room = someRoom,
  messageTest = "Hi there! 👋",
  attachment = DataAttachment(
    file = File("file/path.jpg"),
    name = "file-name" // optional, "file" by default
  )
).let { result ->
  when (result) { // Result<Int, Error>, either the new message id or an error
    is Result.Success -> toast("Message with attachment data published, id is ${result.value}")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

```kotlin
currentUser.sendMessage(
  room = someRoom,
  messageTest = "Hi there! 👋",
  attachment = LinkAttachment(
    link = "https://example.com/image.png",
    type = AttachmentType.IMAGE
  )
).let { result ->
  when (result) { // Result<Int, Error>, either the new message id or an error
    is Result.Success -> toast("Message with attachment link published, id is ${result.value}")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

#### Retrieving attachments

It is possible for users to attach files to messages. If a message has an
attachment you will most likely have to fetch it before you can use it. This
will give you the actual URL of the resource.

### Attachment properties

 | Property      | Type            | Description                                                                                   |
 |---------------|-----------------|-----------------------------------------------------------------------------------------------|
 | link          | String          | The link representing the location of the attachment.                                         |
 | type          | AttachmentType  | The type of the attachment; one of image, video, audio, or file.                              |
 | fetchRequired | Boolean         | If the attachment link needs to be fetched from the Chatkit servers; see Fetch an Attachment. |

### Fetching an attachment

If a message contains an attachment with the `fetchRequired` property set to
`true`, then `attachment.link` cannot be used directly. We must first fetch the
URL of the attachment itself using `fetchAttachment`.

```kotlin
currentUser.fetchAttachment(
  attachmentUrl = message.link
).let { result -> // Result<FetchedAttachment, Error>
  when (result) {
    is Result.Success -> toast("Loaded attachment: ${result.value.link}.")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

## Typing indicators

Sometimes it’s useful to be able to see if another user is typing. You can use
Chatkit to let all the connected members of a room know when another user is
typing.

### Trigger a typing event

To send typing indicator events call `isTypingIn` with the ID of the room in
which current user is typing.

```kotlin
currentUser.isTypingIn(
  roomId = room.id
).let { result -> // Result<Unit, Error>
   when (result) {
     is Result.Success -> toast("Success! Everyone subscribed to the room will know you are typing.")
     is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
   }
 }
```

### Receive typing indicators

To be notified when a user starts or stops typing in a room, listen for either
ChatEvents or RoomEvents.

For `ChatEvent`s across all rooms, register on construction of the
`ChatManager`:

```kotlin
val userResult = chatManager.connect(ChatListeners(
    onUserStartedTyping = { user, room -> toast("User ${user.name} started typing in ${room.name}") },
    onUserStoppedTyping = { user, room -> toast("User ${user.name} stopped typing in ${room.name}") },
    ...
))
```

For events scoped to an individual room, register for `RoomEvent`s when you
subscribe to a room:

```kotlin
currentUser.subscribeToRoom(
  roomId = someroomId,
  listeners = RoomListeners(
    onUserStartedTyping = { user -> toast("User ${user.name} started typing") },
    onUserStoppedTyping = { user -> toast("User ${user.name} stopped typing") },
    ...
  ),
  ...
)
```

## User presence

If a user has at least one active connection to the Chatkit service then they
are considered online. When a user has no active connections they are
considered offline. Each [user object](#users) keeps track of whether a user is
online or offline via the presence property.

```kotlin
if (user.presence is User.Presence.Online) {
  // The user is online! Show an online badge or something...
}
```

Additionally, to be notified when a user comes online or goes offline, you can
provide the `onUserCameOnline` and `onUserWentOffline` listeners or match the
`UserCameOnline` and `UserWentOffline` events. Either at the [room
level](#room-subscription-events) – fires whenever a member of that room goes
on or off line, or at the [connection level](#connecting) – fires whenever any
users sharing a common room membership go on or offline.

```kotlin
chatManager.connect { event ->
  when (event) {
    is UserCameOnline -> toast("User ${event.user.name} came online.")
    is UserWentOffline -> toast("User ${event.user.name} went offline.")
    ...
  }
}
```

```kotlin
currentUser.subscribeToRoom(
  roomId = someroomId,
  listeners = RoomListeners(
    onUserCameOnline = { user -> toast("User ${user.name} started typing") },
    onUserCameOnline = { user -> toast("User ${user.name} stopped typing") },
    ...
  ),
  ...
)
```

## Cursors

Read cursors track how far a user has read through the messages in a room. Each
read cursor belongs to a user and a room – represented by a `Cursor` object.

### Cursor properties

 | Property  | Type             | Description                                                                     |
 |-----------|------------------|---------------------------------------------------------------------------------|
 | position  | String           | The [message](#messages) ID that the user has read up to.                       |
 | updatedAt | String           | The timestamp when the cursor was last set.                                     |
 | room      | Int (room id)    | The [room](#rooms) that the cursor refers to.                                   |
 | user      | String (user id) | The [user](#users) that the cursor belongs to.                                  |
 | type      | Int              | The type of the cursor object, currently always 0 (representing a read cursor). |

### Setting a cursor

When you are confident that the current user has “read” a message, call
`setReadCursor` with a `roomId` and a `position` (the ID of the newest message
that has been “read”).

```kotlin
currentUser.setReadCursor(
  roomId = someRoomId,
  position = someMessageId
).let { result -> // Result<Int, Error>
  when (result) {
    is Result.Success -> toast("Cursor set!")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

### Getting a cursor

The current user’s read cursors are available immediately upon connecting.
Access any existing cursors with the `readCursor` function. (A cursor that
hasn’t been set yet is undefined.)

```kotlin
currentUser.readCursor(
  roomId: someRoomId
)
```

**Note:** To be notified when any of the current user’s read cursors are
changed, supply an `onNewReadCursor` listener on connection or match for
`NewReadCursor` events.

### Access other user's cursors

After subscribing to a room, read cursors for members of that room can be
accessed by supplying a `userId` as the second parameter to the `readCursor`
method.

```kotlin
currentUser.getReadCursor(
  roomId: someRoomId,
  userId: "alice"
).let { result -> // Result<Cursor, Error>
  when (result) {
    is Result.Success -> toast("Cursor: ${result.cursor}!")
    is Result.Failure -> toast("Oops, something bad happened: ${result.error}")
  }
}
```

To be notified when any member of the room changes their read cursor, supply an
`onNewReadCursor` listener [when subscribing to the
room](#room-subscription-events) or match the `NewReadCursor` event.

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

When building this project, you may choose to use a local version of
[`pusher-platform-android`](1).

To do so you can add the path to your local copy in your
`~/.gradle/gradle.properties`:

```
pusher_platform_local=../pusher-platform-android
```

It can be either a relative or absolute path.

[1]: https://github.com/pusher/pusher-platform-android


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
variable. This is, as the name suggests, the GPG passphrase for the Pusher
Maven account.

Now, to do the actual release run:

```bash
gradlew bintrayUpload
```

**Note:** The publish action will both override the current release (if it has
the same version name as the current) and automatically publish the new
version.

### Maven

Syncing the artifacts to Maven is also setup. It involves logging into bintray
and syncing an uploaded release.
