package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.adapters.CommentsAdapter;
import org.meicode.socialmediaapp.adapters.FeedPostsAdapter;
import org.meicode.socialmediaapp.model.PostComments;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

public class CommentsActivity extends AppCompatActivity {

    ImageButton goBackButton;
    ImageButton sendButton;
    RecyclerView recyclerView;
    EditText commentInput;
    CommentsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);
        goBackButton = findViewById(R.id.go_back_button);
        recyclerView = findViewById(R.id.comments_recyclerview);
        commentInput = findViewById(R.id.comment_input);
        sendButton = findViewById(R.id.send_comment_button);

        commentInput.setText("");

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        String userId = getIntent().getExtras().getString("userId");
        String postId = getIntent().getExtras().getString("postId");

        Query query = FirebaseUtil.getPostComments(userId, postId).orderBy("commentTimestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<PostComments> options = new FirestoreRecyclerOptions.Builder<PostComments>().setQuery(query, PostComments.class).build();
        adapter = new CommentsAdapter(options, CommentsActivity.this, postId, userId);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String comment = commentInput.getText().toString().trim();
                if (comment.equals("")) return;

                FirebaseUtil.getUsersCollectionReference().document(FirebaseUtil.getCurrentUserId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String username = documentSnapshot.getString("username");
                        PostComments postComments = new PostComments(comment, Timestamp.now(), FirebaseUtil.getCurrentUserId(), username);

                        FirebaseUtil.getPostComments(userId, postId).add(postComments);
                        FirebaseUtil.getUserPosts(userId).document(postId).update("commentsCount", FieldValue.increment(1));
                        FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId()).document(postId).update("commentsCount", FieldValue.increment(1));

                        commentInput.setText("");
                    }
                });
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}