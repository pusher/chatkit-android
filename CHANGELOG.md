# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/pusher/chatkit-android/compare/1.0.0...HEAD)

### Fixed

- URL Encoding of room IDs when sending typing indicators
- Deserialization of Room privacy (always returned false)
- Deserialization of User customData - was Map<String, String> when other clients were not
  restricted to string values by the API.
- Room name is optional when updating rooms

### Added

- Room objects have a custom data field which can hold a map of arbitrary data,
  in the same way as a User object.

## [1.0.0](https://github.com/pusher/chatkit-android/compare/0.2.4...1.0.0) - 2018-10-30

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
