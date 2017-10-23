# chatkit-android
A rather chatty kit.

🚨 Note - this project is in early preview stage. Use at your own risk. 🚨

The Android client for Pusher Chatkit. If you aren't already here, you can find the source on [Github](https://github.com/pusher/chatkit-android).

For more information the Chatkit service, see [here](http://pusher.com/chatkit). For full documentation, see [here](https://docs.pusher.com/chatkit/overview/)

The SDK is written in Kotlin, but aimed to be as Java-friendly as possible.

## Features

- Creating, joining and deleting rooms
- Adding and removing users to rooms
- Sending and receiving messages to and from rooms
- Seeing who is currently "in" a room
- Seeing who's currently typing in a room

## Setup

### Include it in project

Chatkit is currently distributed as a snapshot, so make sure to have the maven snapshots repository added:

```groovy

```

Then add the library to your app's `dependencies` field in `app/build.gradle`

```groovy

```

## Usage

### Initialise Chatkit


Builder pattern

TokenProvider - testTokenProvider

To learn how to implement a real TokenProvider please see here:



```java

 ChatkitTokenProvider tokenProvider = new ChatkitTokenProvider(
                "https://us1.pusherplatform.io/services/chatkit_token_provider/v1/c090a50e-3e0e-4d05-96b0-a967ee4717ad/token?instance_id=v1:us1:c090a50e-3e0e-4d05-96b0-a967ee4717ad",
                "zan",
                tokenParams,
                new OkHttpClient()
        );

        chatManager = new ChatManager(
                INSTANCE_ID,
                getApplicationContext(),
                tokenProvider,
                null,
                LogLevel.VERBOSE
        );

```


Connect to the ChatManager and implement the `UserSubscriptionListeners`:

```java
chatManager.connect(
                new UserSubscriptionListeners(
                        new CurrentUserListener() {
                            @Override
                            public void onCurrentUser(@NonNull CurrentUser user) {
                                Log.d(TAG, "onCurrentUser");
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

### all the features in the world

Most operations are performed on the `CurrentUser` instance.

List all joined rooms:

Get a room:

Join a room:

Create a room:

Send message:

Subscribe to messages in a room:

Add others to a room:





### Auth!!!

### Typing indicators & stuff





