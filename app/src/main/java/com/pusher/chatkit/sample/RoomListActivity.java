package com.pusher.chatkit.sample;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pusher.chatkit.CurrentUser;
import com.pusher.chatkit.CurrentUserListener;
import com.pusher.chatkit.ErrorListener;
import com.pusher.chatkit.Room;
import com.pusher.chatkit.RoomListener;

import java.util.ArrayList;
import java.util.Collection;

import elements.Error;
import timber.log.Timber;

public class RoomListActivity extends Activity {

    private RecyclerView recyclerView;
    private RoomsAdapter roomsAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);

        Button createNewRoomButton = findViewById(R.id.create_room_btn);

        createNewRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNewRoomDialog();
            }
        });

        roomsAdapter = new RoomsAdapter();
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(roomsAdapter);


        ((MyApplication)getApplication()).getCurrentUser(new CurrentUserListener() {
            @Override
            public void onCurrentUser(@NonNull CurrentUser user) {
                roomsAdapter.addRooms(user.rooms());
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();


    }

    private void showNewRoomDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(RoomListActivity.this);
        builder.setView(R.layout.create_room);

        // Add the buttons
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                final EditText roomTitle = ((AlertDialog) dialog).findViewById(R.id.create_room_title);
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

        ((MyApplication) getApplication()).getCurrentUser(new CurrentUserListener() {
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
            TextView text = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.room_title_text, parent, false);

            return new RoomViewHolder(text);
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

        RoomViewHolder(TextView itemView) {
            super(itemView);
            roomNameView = itemView;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    roomClicked(room);
                }
            });
        }

        void setRoom(Room room) {
            this.room = room;
            String roomTitle = room.getId() + " - " + room.getName();
            roomNameView.setText(roomTitle);
        }
    }
}
