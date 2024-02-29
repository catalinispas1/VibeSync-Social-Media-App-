package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;


import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.adapters.CommentsRecyclerAdapter;
import org.meicode.socialmediaapp.fragments.HomeFragment;
import org.meicode.socialmediaapp.model.PostComments;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    ImageButton goBackButton;
    ImageButton sendButton;
    RecyclerView recyclerView;
    EditText commentInput;
    CommentsRecyclerAdapter adapter;
    DocumentSnapshot lastVisible;
    private static final int PAGE_SIZE = 10;
    List<PostComments> postCommentsList;
    List<PostComments> newCommentsList;
    List<String> commentsId;
    List<String> newCommentsId;
    boolean lastItemReached;
    boolean dataLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);
        goBackButton = findViewById(R.id.go_back_button);
        recyclerView = findViewById(R.id.comments_recyclerview);
        commentInput = findViewById(R.id.comment_input);
        sendButton = findViewById(R.id.send_comment_button);

        commentInput.setText("");
        commentInput.requestFocus();
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        String userId = getIntent().getExtras().getString("userId");
        String postId = getIntent().getExtras().getString("postId");

        HomeFragment.fromCommentActivity = true;
        HomeFragment.userId = userId;
        HomeFragment.postId = postId;

        postCommentsList = new ArrayList<>();
        newCommentsList = new ArrayList<>();
        commentsId = new ArrayList<>();
        newCommentsId = new ArrayList<>();

        adapter = new CommentsRecyclerAdapter(postCommentsList, commentsId, postId, userId, this, newCommentsList, newCommentsId);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        getComments(null, userId, postId);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();

                if (!dataLoading && (firstVisibleItem + visibleItemCount) == totalItemCount && !lastItemReached) {
                    dataLoading = true;
                    getComments(lastVisible, userId, postId);
                    Log.v("TAG", "userFeedCalled because last item scroll detecrted");
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String comment = commentInput.getText().toString().trim();
                if (comment.equals("")) return;

                commentInput.setText("");

                FirebaseUtil.getUsersCollectionReference().document(FirebaseUtil.getCurrentUserId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String username = documentSnapshot.getString("username");
                        PostComments postComments = new PostComments(comment, Timestamp.now(), FirebaseUtil.getCurrentUserId(), username);
                        FirebaseUtil.getPostComments(userId, postId).add(postComments).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                adapter.addNewComment(postComments);
                                adapter.addNewCommentId(documentReference.getId());
                                adapter.notifyItemInserted(0);
                                recyclerView.smoothScrollToPosition(0);
                            }
                        });
                        FirebaseUtil.getUserPosts(userId).document(postId).update("commentsCount", FieldValue.increment(1));
                        FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId()).document(postId).update("commentsCount", FieldValue.increment(1));
                    }
                });
            }
        });
    }
    private void getComments(DocumentSnapshot lastVisible, String userId, String postId) {
        Query query;
        if (lastVisible == null) {
            query = FirebaseUtil.getPostComments(userId, postId)
                    .orderBy("commentTimestamp", Query.Direction.DESCENDING).limit(PAGE_SIZE);
        } else {
            query = FirebaseUtil.getPostComments(userId, postId)
                    .orderBy("commentTimestamp", Query.Direction.DESCENDING).limit(PAGE_SIZE).startAfter(lastVisible);
        }

        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    PostComments postComments = documentSnapshot.toObject(PostComments.class);
                    adapter.addComment(postComments);
                    adapter.addCommentId(documentSnapshot.getId());
                }
                int currentPageSize = queryDocumentSnapshots.size();

                if (currentPageSize > 0) {
                    CommentsActivity.this.lastVisible = queryDocumentSnapshots.getDocuments().get(currentPageSize - 1);
                }

                int startPosition = adapter.getItemCount() - currentPageSize;
                int endPosition = adapter.getItemCount() - 1;

                dataLoading = false;

                adapter.notifyItemRangeInserted(startPosition, endPosition);

                if (currentPageSize < PAGE_SIZE) {
                    lastItemReached = true;
                }
            }
        });
    }
}