package com.pusher.chatkit.sample;

import android.content.DialogInterface;
import android.content.Intent;
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
import com.pusher.chatkit.OnCompleteListener;
import com.pusher.chatkit.Room;
import com.pusher.chatkit.RoomListener;
import com.pusher.chatkit.RoomSubscriptionListenersAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import elements.Error;
import timber.log.Timber;

public class ListRoomsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RoomsAdapter roomsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_rooms);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        roomsAdapter = new RoomsAdapter();
        recyclerView = findViewById(com.pusher.chatkit.sample.R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(roomsAdapter);

        ((ChatApplication)getApplication()).getCurrentUser(new CurrentUserListener() {
            @Override
            public void onCurrentUser(@NonNull CurrentUser currentUser) {
                roomsAdapter.addRooms(currentUser.rooms());
            }
        });



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNewRoomDialog();
            }
        });
    }

    private void showNewRoomDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(com.pusher.chatkit.sample.R.layout.create_room);

        // Add the buttons
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                final EditText roomTitle = ((AlertDialog) dialog).findViewById(com.pusher.chatkit.sample.R.id.create_room_title);
                createNewRoom(roomTitle.getText().toString());
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

    private void roomClicked(Room room){
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.EXTRA_ROOM_ID, room.getId());
        startActivity(intent);
    }

    private void createNewRoom(final String name){

        ((ChatApplication) getApplication()).getCurrentUser(new CurrentUserListener() {
            @Override
            public void onCurrentUser(@NonNull CurrentUser user) {

                user.createRoom(
                        name,
                        new RoomListener() {
                            @Override
                            public void onRoom(Room room) {
                                roomsAdapter.addRoom(room);
                                Timber.d("ROOM CREATED! %s", room );
                            }
                        }, new ErrorListener() {
                            @Override
                            public void onError(Error error) {
                                Timber.d("Error creating room! %s", error);

                            }
                        });
            }
        });


    }


    class RoomsAdapter extends RecyclerView.Adapter<RoomViewHolder> {

        public void addRooms(Collection<Room> rooms){
            this.rooms.addAll(rooms);
            notifyDataSetChanged();
        }

        public void addRoom(Room room){
            this.rooms.add(room);
            notifyDataSetChanged();
        }

        private ArrayList<Room> rooms = new ArrayList<>();

        @Override
        public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewGroup roomItem = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.room_list_item, parent, false);
            return new RoomViewHolder(roomItem);
        }

        @Override
        public void onBindViewHolder(RoomViewHolder holder, int position) {
            holder.setRoom(rooms.get(position));
        }

        @Override
        public int getItemCount() {
            return rooms.size();
        }
    }

    class RoomViewHolder extends RecyclerView.ViewHolder {

        private Room room;
        TextView roomNameView;
        TextView roomIdView;

        RoomViewHolder(View itemView) {
            super(itemView);

            roomNameView = itemView.findViewById(R.id.room_name);
            roomIdView = itemView.findViewById(R.id.room_id);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    roomClicked(room);
                }
            });
        }

        void setRoom(Room room) {
            this.room = room;
            roomNameView.setText(room.getName());
            roomIdView.setText(String.valueOf(room.getId()));
        }
    }

}
