# chatkit-android

The Android client for Pusher Chatkit. If you aren't already here, you can find the source on [Github](https://github.com/pusher/chatkit-android).

For more information on the Chatkit service, see [here](http://pusher.com/chatkit). For full documentation, see [here](https://docs.pusher.com/chatkit/overview/)

The SDK is written in Kotlin, but aimed to be as Java-friendly as possible.

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

    api 'com.pusher:chatkit-android:0.1.0'
}
```

## Usage

### Initialise Chatkit


Builder pattern

TokenProvider - testTokenProvider

We provide you with a sample token provider implementation. You can enable / disable it in the dashboard.
To include it in your application, create it with your details, as such:


```java

ChatkitTokenProvider tokenProvider = new ChatkitTokenProvider(TOKEN_PROVIDER_ENDPOINT);

ChatManager chatManager = new ChatManager(
    INSTANCE_LOCATOR, 
    USER_NAME,
    new AndroidChatkitDependencies(
        getApplicationContext(),
        tokenProvider
    )
);

```


Connect to the ChatManager and implement the `UserSubscriptionListeners`:

```java
chatManager.connect(
    new UserSubscriptionListeners(
        new CurrentUserListener() {
            @Override
            public void currentUserReceived(@NonNull CurrentUser user) {
                Log.d(TAG, "currentUserReceived");
                currentUser = user;

                joinOrCreateRoom();
            }
        },
        new ErrorListener() {
            @Override
            public void onError(Error error) {
                Log.d(TAG, "onError");
            }
        },
        new RemovedFromRoomListener() {
            @Override
            public void removedFromRoom(Room room) {
                Log.d(TAG, "removed from room");
            }
        }
    )
);
```

### Features breakdown

Most operations are performed on the `CurrentUser` instance.
It has the following params, with `CustomData` being an a Map of any extra params it might have set:

```kotlin
class CurrentUser(
    val id: String,
    val createdAt: String,
    var updatedAt: String,
    var name: String?,
    var avatarURL: String?,
    var cursors: Map<Int, Cursor>
    var customData: CustomData?
)

typealias CustomData = MutableMap<String, String>
```

List all joined rooms:

```java
Set<Room> rooms = user.rooms();
```

Where `Room` is a Kotlin data class with the following parameters:

```kotlin
data class Room(
    val id: Int,
    val createdById: String,
    var name: String,
    var isPrivate: Boolean,
    val createdAt: String,
    var updatedAt: String,
    var deletedAt: String,
    var memberUserIds: MutableList<String>,
    private var userStore: UserStore?
){
    fun userStore(): UserStore {
        if(userStore == null) userStore = UserStore()
        return userStore!!
    }

    fun removeUser(userId: String){
        memberUserIds.remove(userId)
        userStore().remove(userId)
    }
}
```

Get a room:

```java
Room room = currentUser.getRoom(roomId);
```

Join a room:

```java
currentUser.joinRoom(
    roomId,
    new RoomListener() {
        @Override
        public void onRoom(Room room) {

        }
    }, new ErrorListener() {
        @Override
        public void onError(Error error) {

        }
    }
);
```

Create a room:

```java
user.createRoom("roomName",
    new RoomListener() {
        @Override
        public void onRoom(Room room) {

        }
    }, new ErrorListener() {
        @Override
        public void onError(Error error) {

        }
    }
);
```


Send message:

```java
currentUser.sendMessage(
    room.getId(),
    "Hey there!",
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

You can also send messages with attachments. In the following example we assume that `getAttachmentFile` is a function that returns a `File` object.

```java
File myAttachment = getAttachmentFile();
String chosenFilename = "testing.png";

currentUser.sendMessage(
    room.getId(),
    "Hey there!",
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

Subscribe to receive messages in a room:

```java
 currentUser.subscribeToRoom(room, new RoomSubscriptionListenersAdapter(){
    @Override
    public void onNewMessage(Message message) {

    }

    @Override
    public void onError(Error error) {

    }
});
```

Or subscribe both to messages and to cursors:

```java

currentUser.subscribeToRoom(
    room,
    20,
    new RoomSubscriptionListenersAdapter() {
        @Override
        public void onNewMessage(Message message) {

        }

        @Override
        public void onError(Error error) {

        }
    },
    new CursorsSubscriptionListenersAdapter() {
        @Override
        public void onCursorSet(Cursor cursor) {

        }

        @Override
        public void onError(Error error) {

        }
    }
);
```

Set your cursor in a room:

```java
currentUser.setCursor(
    someMessage.getId(),
    room,
    new SetCursorListener() {
        @Override
        public void onSetCursor() {

        }
    },
    new ErrorListener() {
        @Override
        public void onError(Error error) {

        }
    }
);
```

Get all your cursors (as a map, indexed by room ID):

```java
currentUser.getCursors();
```

Add others to a room:

```java
currentUser.addUsers(
    room.getId(),
    new String[]{"zan", "ham", "vivan"},
    new OnCompleteListener() {
        @Override
        public void onComplete() {

        }
    },
    new ErrorListener() {
        @Override
        public void onError(Error error) {

        }
    }
);
```

### Implementing a Token Provider

In a non-testing application you will need to implement your own Token Provider endpoint, and provide it to the `ChatkitTokenProvider` constructor.
You can read more about how authentication flow works on the documentation site for it: https://docs.pusher.com/chatkit/authentication/#the-authentication-flow

The core interface to implement on the client is this:

```java
ChatkitTokenProvider tokenProvider = new ChatkitTokenProvider(TOKEN_PROVIDER_ENDPOINT);
```

### Who's Online

You will get informed of who is currently online by attaching listeners to the `ChatManager` object.

The methods to implement are defined in `PresenceSubscriptionListeners`:

```java
public interface PresenceSubscriptionListeners {
    void userCameOnline(User user);
    void userWentOffline(User user);
}
```

### Development build

When building this project, you may choose to use a local version of [`pusher-platform-android`](1). 

To do so you can add the path to your local copy in your `~/.gradle/gradle.properties`:

````
pusher_platform_local=../pusher-platform-android
````

It can be either a relative or absolute path.

[1]: https://github.com/pusher/pusher-platform-android