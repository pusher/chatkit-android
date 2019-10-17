# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/pusher/chatkit-android/compare/v1.8.1...HEAD)

## [1.8.1](https://github.com/pusher/chatkit-android/compare/v1.8.0...v1.8.1)

### Fixed

- Ensure we parse the new cursor in a user subscription event correctly. This issue was
preventing any future room subscription events from being called after a set cursor.

## [1.8.0](https://github.com/pusher/chatkit-android/compare/v1.7.2...v1.8.0)

### Fixed

- Speed up subscribing to a room with many users.
- Prevent `onUserJoined` being called when a user firsts subscribes to a room for
people who are already in the room. This behavior is now consistent with our other Client SDKs
  - To get the members of a room, you need to call `usersForRoom` with either the room, or roomId
- Ensure that multiple calls for many users presence is called in parallel instead of synchronously

## Added

- Fetch all `usersForRoom` by roomId

## [1.7.2](https://github.com/pusher/chatkit-android/compare/v1.7.1...v1.7.2) - 2019-09-05

### Changed

- Uses latest Pusher Beams SDK (1.4.5) to power push notifications

## [1.7.1](https://github.com/pusher/chatkit-android/compare/v1.7.0...v1.7.1) - 2019-08-22

### Fixed

- Ensure we pass back correct a correct list of member user ids if the room changes

## [1.7.0](https://github.com/pusher/chatkit-android/compare/v1.6.0...v1.7.0) - 2019-08-01

### Added

- Support for `PushNotificationTitleOverride` field in the Room model

### Changed

- Uses latest Pusher Beams SDK (1.4.4) to power push notifications

## [1.6.0](https://github.com/pusher/chatkit-android/compare/v1.5.0...v1.6.0) - 2019-07-16

No changes in this version. Previous versions from 1.4.0 onwards were released incorrectly
and this release rectifies the issue. Please upgrade to 1.6.0 if you were previously on
1.4.0 or 1.5.0.

## [1.5.0](https://github.com/pusher/chatkit-android/compare/v1.4.0...v1.5.0) - 2019-07-03

## Changed

- Support for user provided room IDs. `createRoom` now takes an extra `id` parameter that
  allows supplying a string room ID. If an `id` is not provided, the server will return a
  generated string ID that are not numeric.
- Moves tha api version to v6.

## [1.4.0](https://github.com/pusher/chatkit-android/compare/v1.3.4...v1.4.0) - 2019-06-24

## Added

- Support for `message_deleted` events via `onMessageDeleted` hook on rooms.
- Moved to using version v5 of the api.

## [1.3.4](https://github.com/pusher/chatkit-android/compare/v1.3.3...v1.3.4) - 2019-06-20

## Changed

- Uses latest Pusher Beams SDK (1.4.3) to power push notifications

## [1.3.3](https://github.com/pusher/chatkit-android/compare/v1.3.2...v1.3.3) - 2019-05-09

## Fixed

- Wrap exceptions in callback based interface in `Result` and return them

## [1.3.2](https://github.com/pusher/chatkit-android/compare/v1.3.1...v1.3.2) - 2019-05-03

## Fixed

- Missing signatures for CurrentUser multipart subscriptions added.

## [1.3.1](https://github.com/pusher/chatkit-android/compare/v1.3.0...v1.3.1) - 2019-04-25

## Fixed

- Missing signatures for `getReadCursor(room, user)` exposed

## [1.3.0](https://github.com/pusher/chatkit-android/compare/v1.2.0...v1.3.0) - 2019-04-24

## Added

- Room entities have two new properties:
  - `unreadCount` - the number of unread messages in the room
  - `lastMessageAt` - timestamp of the most recent message in the room

Both are nullable, because you do not have access to the messages for a room
unless you are a member. The last message timestamp will also be `null` if
there are no messages in the room.

When you are a member of a room, these values are available for that room
immediately on initial connection and real-time changes to them are
communicated via room updated events.

There is no need to subscribe to a room to recieve them.

## [1.2.0](https://github.com/pusher/chatkit-android/compare/v1.1.1...v1.2.0) - 2019-03-08

## Added

- Multipart messaging support:
  - `sendMultipartMessage`, `sendSimpleMessage`, `subscribeToRoomMultipart` and
    `fetchMultipartMessages` methods
  - `onMultipartMessage` listener

## Deprecated

- `sendMessage`, `subscribeToRoom` and `fetchMessages` are deprecated in favour of
  their multipart counterparts. They will be removed in a future major release.

## [1.1.1](https://github.com/pusher/chatkit-android/compare/v1.1.0...v1.1.1) - 2019-02-06

## Fixed

- Emit correct events when reconciling state after transient disconnections
- Do not emit events before initial connection, or room subscriptions are complete

## [1.1.0](https://github.com/pusher/chatkit-android/compare/v1.0.4...v1.1.0)

## Added

- Support for Push Notifications

## Fixed

- Presence state is marked offline as soon as the app is placed in the
  background, not when it is killed by the OS

## [1.0.5](https://github.com/pusher/chatkit-android/compare/v1.0.4...v1.0.5) - 2018-11-23

- `Attachment` now contains an additional `name` attribute pointing to the name of the file.
- Removed all logic related to `FetchedAttachment` since links returned by the server do not
  require an additional "fetch" step.

## [1.0.4](https://github.com/pusher/chatkit-android/compare/v1.0.3...v1.0.4) - 2018-11-21

## Fixed

- Fixed sequence of catch up events generated when `RoomUpdated` is received when
  client loses connectivity. It will now only generate a `RoomUpdated` event
  when a room's properties change.
- Fixed `close` and `connect` in `ChatManager`. Previously calling `close` and then
  `connect` again would not create a new connection. Doing so now will close the existing
  connection and create a new one.

## [1.0.3](https://github.com/pusher/chatkit-android/compare/v1.0.2...v1.0.3) - 2018-11-13

### Fixed

- Fixed deserialisation of the `avatarURL` field on User objects. Was
  previously always `null`.

## [1.0.2](https://github.com/pusher/chatkit-android/compare/v1.0.1...v1.0.2)

### Fixed

- Upgrade pusher-platform-android to get fix for subscription reconnections

## [1.0.1](https://github.com/pusher/chatkit-android/compare/v1.0.0...v1.0.1) - 2018-10-31

### Fixed

- URL Encoding of room IDs when sending typing indicators
- Deserialization of `Room` privacy (always returned false)
- Deserialization of `User` `customData` - was `Map<String, String>` when other clients were not
  restricted to string values by the API.
- Room name is optional when updating rooms

### Added

- Room objects have a custom data field which can hold a map of arbitrary data,
  in the same way as a User object.

## [1.0.0](https://github.com/pusher/chatkit-android/compare/0.2.4...v1.0.0) - 2018-10-30

### Rewritten

The library has been substantially reworked, and the public interface has
changed in various ways. Refer to the full documentation at
https://docs.pusher.com/chatkit/reference/android

A summary of changes:

### Interface

- The public interface now defaults to accepting callback functions rather than
  returning `Future`s.
- A synchronous interface is optionally available.
- Several event types have been renamed
  - RoomSubscriptionEvent -> RoomEvent
  - ChatManagerEvent -> ChatEvent
  - Members of these types (the events and handlers) have also had naming tweaks
- The `fetchAttachment` method has been removed. It is no longer required, as we
  no longer serve attachment URLs which require redirection. All URLs returned
  point directly the the content.
- Room IDs are now modelled as `String`s instead of `Int`s

### Semantics

- Room memberships are not available until the room has been subscribed to.
- Initial connection is not blocked on availability of presence states.
  - You can identify events representing the initial state of a user's presence
    because the `prevState` in the event will be `Unknown`. This can be used to
    gracefully populate the UI without triggering effects which might be tied
    to an `Offline` -> `Online` transition.
  - Presence states are fetched as users become known to the client, i.e. as
    room subscriptions are started, which reveal the room memberships.

## [0.2.4](https://github.com/pusher/chatkit-android/compare/0.2.3...0.2.4) - 2018-07-27

### Fixed

- Made Maven release process work properly

## [0.2.3](https://github.com/pusher/chatkit-android/compare/0.2.2...0.2.3) - 2018-07-23

### Added

- Added `user` property to `Message`

## [0.2.2](https://github.com/pusher/chatkit-android/compare/0.2.1...0.2.2) - 2018-06-15

### Fixed

- Messages with attachments that require a fetch are now identified properly
  over room subscriptions and when fetching messages

### Changed

- `forEach`s replaced with `for (... in ...)`s

## [0.2.1](https://github.com/pusher/chatkit-android/compare/0.2.0...0.2.1) - 2018-05-29

### Fixed

- `fetchMessages` now correctly sets the `initialId` query param

### Changed

- Bump PusherPlatform dependency to 0.4.1

## [0.2.0](https://github.com/pusher/chatkit-android/compare/0.1.0...0.2.0) - 2018-05-18

### Changed

- Bump PusherPlatform dependency to 0.4.0
- Requests resulting from `setReadCursor` calls are batched up if made in quick succession
- File upload path has been updated to include the user ID

### Removed

- Removed `tokenParams`

### Added

- Added tests for nearly everything

## [0.1.0](https://github.com/pusher/chatkit-android/compare/919c62d03e...0.1.0) - 2018-01-26

### Changed

- Bump PusherPlatform dependency to 0.3.0
- `addMessage` on `CurrentUser` has been replaced by `sendMessage`

### Added

- Support for message attachments
- `sendMessage` on `CurrentUser`, which replaces `addMessage`; usage looks like this:

```java
currentUser.sendMessage(
    myRoom.getId(),
    "Hey there",
    new MessageSentListener() {
        @Override
        public void onMessage(int messageId) {

        }
    }, new ErrorListener() {
        @Override
        public void onError(Error error) {

        }
    }
);
```

Note that the room's ID is now required as a parameter, not the whole `Room` object as was the case with `addMessage`

- `sendMessage` supports sending messages with an attachment. In the following example we assume that `getAttachmentFile` is a function that returns a `File` object.

```java
File myAttachment = getAttachmentFile();
String chosenFilename = "testing.png";

currentUser.sendMessage(
    myRoom.getId(),
    "Hey there",
    new DataAttachment(myFile, chosenFilename),
    new MessageSentListener() {
        @Override
        public void onMessage(int messageId) {

        }
    }, new ErrorListener() {
        @Override
        public void onError(Error error) {

        }
    }
);
```

There are currently 2 different types of attachment supported:

* `DataAttachment(val file: File, val name: String)`: Use this if you have your file as a `File` and you want it to be stored by the Chatkit servers. The `name` parameter is the name that the file will be given when it is stored by our servers.
* `LinkAttachment(val link: String, val type: String)`: Use this if you have a file stored elsewhere that you would like to attach to a message without it being uploaded to and stored by the Chatkit servers. The `type` `parameter` currently needs to be one of `"image"`, `"video"`, `"audio"`, or `"file"`. This will likely eventually be encoded in an `enum` but for now we're leaving it as just a `String` while we finalise the API.

Here's an example of using a `LinkAttachment`:

```java
currentUser.sendMessage(
    myRoom.getId(),
    "Hey there",
    new LinkAttachment("https://i.giphy.com/PYEGoZXABBMuk.gif", "image"),
    new MessageSentListener() {
        @Override
        public void onMessage(int messageId) {

        }
    }, new ErrorListener() {
        @Override
        public void onError(Error error) {

        }
    }
);
```

- `Message`s now have an optional `attachment` property of type `Attachment?`. `Attachment` looks like this:

```kotlin
data class Attachment(
    var fetchRequired: Boolean = false,
    val link: String,
    val type: String
)
```

If `fetchRequired` is `true` then it means that the attachment is stored on the Chatkit servers and you need to make a request to the Chatkit API to fetch a valid link. To do this you can use the `fetchAttachment` function that has been added to the `CurrentUser` class. You use that like this:

```java
currentUser.fetchAttachment(
    message.getAttachment().getLink(),
    new FetchedAttachmentListener() {
        @Override
        public void onFetch(FetchedAttachment attachment) {

        }
    },
    new ErrorListener() {
        @Override
        public void onError(Error error) {

        }
    }
);
```

You can then use the `attachment.link` to download the file, if you so wish.
