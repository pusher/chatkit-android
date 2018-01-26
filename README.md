# chatkit-android
A rather chatty kit.

🚨 Note - this project is in early preview stage. Use at your own risk. 🚨

The Android client for Pusher Chatkit. If you aren't already here, you can find the source on [Github](https://github.com/pusher/chatkit-android).

For more information on the Chatkit service, see [here](http://pusher.com/chatkit). For full documentation, see [here](https://docs.pusher.com/chatkit/overview/)

The SDK is written in Kotlin, but aimed to be as Java-friendly as possible.

## Features

- Creating, joining and deleting rooms
- Adding and removing users to rooms
- Sending and receiving messages to and from rooms
- Seeing who is currently in a room
- Seeing who's currently online

## Setup

### Include it in project

Chatkit is currently distributed as a snapshot, so make sure to have the maven snapshots repository added:

```groovy
allprojects {
    repositories {
        google()
        jcenter()
    }
}

```

Then add the library to your app's `dependencies` field in `app/build.gradle`

```groovy
dependencies {
    ...
    api 'com.pusher:chatkit:0.0.1-SNAPSHOT'
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

ChatManager chatManager = new ChatManager.Builder()
                        .instanceLocator(INSTANCE_LOCATOR)
                        .userId(USER_NAME)
                        .context(getApplicationContext())
                        .tokenProvider(tokenProvider)
                        .build();

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
                        room, 
                        new RoomListener() {
                            @Override
                            public void onRoom(Room room) {
        
                            }
                        }, new ErrorListener() {
                            @Override
                            public void onError(Error error) {
                                
                            }
                });

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
                });
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
                });
```


Send message:

```java
                currentUser.addMessage("Hello, world!", room, new MessageSentListener() {
                    @Override
                    public void onMessage(int messageId) {

                    }
                }, new ErrorListener() {
                    @Override
                    public void onError(Error error) {

                    }
                });
```

Subscribe to messages in a room:

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
currentUser.addUsers(room.getId(), new String[]{"zan", "ham", "vivan"}, new OnCompleteListener() {
                    @Override
                    public void onComplete() {

                    }
                }, new ErrorListener() {
                    @Override
                    public void onError(Error error) {
                        
                    }
                });
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

