package org.meicode.socialmediaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

import org.meicode.socialmediaapp.adapters.FeedPostsAdapter;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

public class PostViewActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    FeedPostsAdapter adapter;
    ImageButton goBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_view);
        recyclerView = findViewById(R.id.post_view_recyclerview);
        goBackButton = findViewById(R.id.go_back_button);

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        String userId = getIntent().getExtras().getString("userId");
        int position = getIntent().getIntExtra("position", 0);

        Query query = FirebaseUtil.getUserPosts(userId).orderBy("uploadTime", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<PostModel> options = new FirestoreRecyclerOptions.Builder<PostModel>().setQuery(query, PostModel.class).build();

        adapter = new FeedPostsAdapter(options, PostViewActivity.this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        recyclerView.scrollToPosition(position);
    }
}