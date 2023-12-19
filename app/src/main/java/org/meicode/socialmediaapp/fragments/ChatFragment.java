package org.meicode.socialmediaapp.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.adapters.RecentChatAdapter;
import org.meicode.socialmediaapp.adapters.SearchUserRecyclerAdapter;
import org.meicode.socialmediaapp.model.ChatRoomModel;
import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

public class ChatFragment extends Fragment {

    RecyclerView recyclerView;
    RecentChatAdapter adapter;
    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.recent_chats);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        Query query = FirebaseUtil.allChatroomCollectionReference().whereArrayContains("userIds", FirebaseUtil.getCurrentUserId())
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatRoomModel> options = new FirestoreRecyclerOptions.Builder<ChatRoomModel>()
                .setQuery(query, ChatRoomModel.class).build();

        adapter = new RecentChatAdapter(options, getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }
}