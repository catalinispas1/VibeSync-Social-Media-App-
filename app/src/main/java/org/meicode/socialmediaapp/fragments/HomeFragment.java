package org.meicode.socialmediaapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.SplashActivity;
import org.meicode.socialmediaapp.adapters.FeedPostsAdapter;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.List;

public class HomeFragment extends Fragment {

    RecyclerView feedRecyclerView;
    FeedPostsAdapter adapter;
    TextView emptyFeedTextview;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        feedRecyclerView = view.findViewById(R.id.feed_recyclerview);
        emptyFeedTextview = view.findViewById(R.id.empty_feed_textview);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SplashActivity.firstTimeLaunch) {
            getUserFeed();
            SplashActivity.firstTimeLaunch = false;
        }
    }

    int querySize;
    private void getUserFeed() {
        Query query = FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId())
                .orderBy("uploadTime", Query.Direction.DESCENDING);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.getResult().isEmpty()) {
                    emptyFeedTextview.setVisibility(View.VISIBLE);
                } else emptyFeedTextview.setVisibility(View.GONE);
                querySize = task.getResult().size();
            }
        });

        FirestoreRecyclerOptions<PostModel> options = new FirestoreRecyclerOptions.Builder<PostModel>().setQuery(query, PostModel.class).build();
        adapter = new FeedPostsAdapter(options, getContext());
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        feedRecyclerView.setAdapter(adapter);
        adapter.startListening();

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                feedRecyclerView.smoothScrollToPosition(0);
            }
        });
    }
}