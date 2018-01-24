package com.pusher.chatkit.sample;

import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.database.Cursor;

import com.pusher.chatkit.CurrentUser;
import com.pusher.chatkit.CurrentUserListener;
import com.pusher.chatkit.CursorsSubscriptionListenersAdapter;
import com.pusher.chatkit.DataAttachment;
import com.pusher.chatkit.ErrorListener;
import com.pusher.chatkit.FetchedAttachment;
import com.pusher.chatkit.FetchedAttachmentListener;
import com.pusher.chatkit.Message;
import com.pusher.chatkit.MessageSentListener;
import com.pusher.chatkit.Room;
import com.pusher.chatkit.RoomSubscriptionListenersAdapter;
import com.pusher.chatkit.SetCursorListener;
import com.pusher.chatkit.User;

import com.pusher.platform.Cancelable;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import elements.Error;
import timber.log.Timber;

public class ChatRoomActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = ChatRoomActivity.class.getName() + "ExtraRoomId";
    private static final int PICK_IMAGE_REQUEST = 1;
    private MessagesAdapter messagesAdapter;
    private RecyclerView recyclerView;
    private int roomId;
    private File selectedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNewMessageDialog();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        messagesAdapter = new MessagesAdapter();
        recyclerView = findViewById(R.id.messages_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messagesAdapter);

        roomId = getIntent().getIntExtra(EXTRA_ROOM_ID, -1);

        ((ChatApplication)getApplication()).getCurrentUser(new CurrentUserListener() {
            @Override
            public void onCurrentUser(final CurrentUser user) {
                final Room room = user.getRoom(roomId);
                setTitle(room.getName());
                user.subscribeToRoom(
                        room,
                        20,
                        new RoomSubscriptionListenersAdapter() {
                            @Override
                            public void onNewMessage(final Message message) {
                                Timber.d("New message: %s", message);
                                if (user.getCursors().get(roomId) != null) {
                                    final int position = user.getCursors().get(roomId).getPosition();
                                    Timber.d("Current cursor: %s", position);
                                    final int messageId = message.getId();
                                    if (messageId > position) {
                                        user.setCursor(
                                                messageId,
                                                room,
                                                new SetCursorListener() {
                                                    @Override
                                                    public void onSetCursor() {
                                                        Timber.d("set cursor to: %s", messageId);
                                                    }
                                                },
                                                new ErrorListener() {
                                                    @Override
                                                    public void onError(Error error) {
                                                        Timber.d("error setting cursor!");
                                                    }
                                                }
                                        );
                                    }
                                }
                                messagesAdapter.addMessage(message);
                                if (message.getAttachment() != null && message.getAttachment().getFetchRequired()) {
                                    user.fetchAttachment(
                                            message.getAttachment().getLink(),
                                            new FetchedAttachmentListener() {
                                                @Override
                                                public void onFetch(FetchedAttachment attachment) {
                                                    Log.d("PC", attachment.getLink());
                                                }
                                            },
                                            new ErrorListener() {
                                                @Override
                                                public void onError(Error error) {
                                                    Log.d("PC", error.toString());
                                                }
                                            }
                                    );
                                }
                            }

                            @Override
                            public void onError(Error error) {
                                Timber.e("Error subscribing to room! %s", error);
                            }
                        },
                        new CursorsSubscriptionListenersAdapter() {
                            @Override
                            public void onCursorSet(com.pusher.chatkit.Cursor cursor) {
                                Timber.d(
                                        "%s's cursor was set: %s",
                                        cursor.getUser().getName(),
                                        cursor.getPosition()
                                );
                            }

                            @Override
                            public void onError(Error error) {
                                Timber.e("Error subscribing to cursors! %s", error);
                            }
                        }
                );
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, @NotNull Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST) {
            Uri uri = data.getData();
            Context context = this.getApplicationContext();
            String filePath = getPath(context, uri);

            if (filePath != null) {
                File file = new File(filePath);
                this.selectedFile = file;
            } else {
                Log.d("PC", "File path is null");
            }
        }
    }

    public final void throwUpThePicker() {
        Intent pickPhoto = new Intent("android.intent.action.GET_CONTENT");
        pickPhoto.setType("image/*");
        this.startActivityForResult(pickPhoto, PICK_IMAGE_REQUEST);
    }

    public static String getPath(final Context context, final Uri uri)  {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,  String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private void showNewMessageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatRoomActivity.this);
        builder.setView(R.layout.insert_message);

        // Add the buttons
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final EditText messageText = ((AlertDialog) dialog).findViewById(R.id.editText);

                if (selectedFile != null) {
                    enterMessage(messageText.getText().toString(), new DataAttachment(selectedFile, "testing.png"));
                } else {
                    ((ChatApplication) getApplication()).getCurrentUser(new CurrentUserListener() {
                        @Override
                        public void onCurrentUser(@NonNull CurrentUser user) {

                            Room room = user.getRoom(roomId);
                            user.sendMessage(room.getId(), messageText.getText().toString(), null, new MessageSentListener() {
                                @Override
                                public void onMessage(int messageId) {
                                    Timber.d("Message sent without attachment! %d", messageId);

                                }
                            }, new ErrorListener() {
                                @Override
                                public void onError(Error error) {
                                    Timber.d("Error sending message without attachment! %s", error);
                                }
                            });
                        }
                    });
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setNeutralButton("Add picture", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean closeDialog = false;
                if (closeDialog) {
                    dialog.dismiss();
                }

                throwUpThePicker();
            }
        });
    }

    private void enterMessage(final String message, final DataAttachment attachment) {
        ((ChatApplication) getApplication()).getCurrentUser(new CurrentUserListener() {
            @Override
            public void onCurrentUser(@NonNull CurrentUser user) {

                Room room = user.getRoom(roomId);
                user.sendMessage(room.getId(), message, attachment, new MessageSentListener() {
                    @Override
                    public void onMessage(int messageId) {
                        Timber.d("Message sent! %d", messageId);

                    }
                }, new ErrorListener() {
                    @Override
                    public void onError(Error error) {
                        Timber.d("Error sending message! %s", error);
                    }
                });
            }
        });
    }

    private class MessagesAdapter extends RecyclerView.Adapter<MessageViewHolder>{

        List<Message> messages = new ArrayList<>();

        void addMessage(Message newMessage){
            messages.add(newMessage);
            notifyItemInserted(messages.size() -1);
            recyclerView.scrollToPosition(messages.size() -1);
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(MessageViewHolder holder, int position) {
            Message message = messages.get(position);
            holder.setMessage(message);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }

    private class MessageViewHolder extends RecyclerView.ViewHolder{
        TextView nameText;
        TextView messageText;
        Message message;

        public MessageViewHolder(View itemView) {
            super(itemView);

            nameText = itemView.findViewById(R.id.sender_name);
            messageText = itemView.findViewById(R.id.message_text);
        }

        void setMessage(Message message){
            this.message = message;
            User sender = message.getUser();

            if(sender != null && sender.getName() != null) nameText.setText(sender.getName());
            else nameText.setText(message.getUserId());

            messageText.setText(message.getText());
        }
    }
}
