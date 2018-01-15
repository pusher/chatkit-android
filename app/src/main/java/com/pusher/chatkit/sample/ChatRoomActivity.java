package com.pusher.chatkit.sample;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.pusher.chatkit.CurrentUser;
import com.pusher.chatkit.CurrentUserListener;
import com.pusher.chatkit.ErrorListener;
import com.pusher.chatkit.Message;
import com.pusher.chatkit.MessageSentListener;
import com.pusher.chatkit.Room;
import com.pusher.chatkit.RoomSubscriptionListenersAdapter;
import com.pusher.chatkit.User;

import java.util.ArrayList;
import java.util.List;

import elements.Error;
import timber.log.Timber;

public class ChatRoomActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = ChatRoomActivity.class.getName() + "ExtraRoomId";
    private MessagesAdapter messagesAdapter;
    private RecyclerView recyclerView;
    private int roomId;

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
            public void onCurrentUser(@NonNull CurrentUser user) {
                Room room = user.getRoom(roomId);
                setTitle(room.getName());
                user.subscribeToRoom(room, new RoomSubscriptionListenersAdapter() {
                    @Override
                    public void onNewMessage(Message message) {
                        Timber.d("New message: %s", message);
                        messagesAdapter.addMessage(message);
                    }

                    @Override
                    public void onError(Error error) {
                        Timber.e("Error subscribing to room! %s", error);
                    }
                });
            }
        });
    }


    private void showNewMessageDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(ChatRoomActivity.this);
        builder.setView(R.layout.insert_message);

        // Add the buttons
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                final EditText messageText = ((AlertDialog) dialog).findViewById(R.id.editText);
                enterMessage(messageText.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void enterMessage(final String message) {
        ((ChatApplication) getApplication()).getCurrentUser(new CurrentUserListener() {
            @Override
            public void onCurrentUser(@NonNull CurrentUser user) {

                Room room = user.getRoom(roomId);
                user.addMessage(message, room, new MessageSentListener() {
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
