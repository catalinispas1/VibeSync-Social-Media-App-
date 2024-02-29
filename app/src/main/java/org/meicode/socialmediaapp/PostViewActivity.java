package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.adapters.FeedPostsRecyclerAdaper;
import org.meicode.socialmediaapp.fragments.HomeFragment;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

public class PostViewActivity extends AppCompatActivity {

    ImageButton goBackButton;
    RecyclerView recyclerView;
    FeedPostsRecyclerAdaper adapter;
    List<PostModel> postModelList;
    DocumentSnapshot lastVisible;
    List<String> postsIds;
    String userId;
    boolean dataLoading;
    boolean lastItemReached;
    private static final int PAGE_SIZE = 3;

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

        userId = getIntent().getExtras().getString("userId");
        int position = getIntent().getIntExtra("position", 0);

        postModelList = new ArrayList<>();
        postsIds = new ArrayList<>();

        adapter = new FeedPostsRecyclerAdaper(postModelList, postsIds, this, recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        getUserPosts(null, position);

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
                    getUserPosts(lastVisible, position);
                    Log.v("TAG", "userFeedCalled because last item scroll detecrted");
                }
            }
        });
    }

    private void getUserPosts(DocumentSnapshot lastVisible, int position) {
        Query query;

        if (lastVisible == null) {
            query = FirebaseUtil.getUserPosts(userId)
                    .orderBy("uploadTime", Query.Direction.DESCENDING).limit(PAGE_SIZE + position);
        } else {
            query = FirebaseUtil.getUserPosts(userId)
                    .orderBy("uploadTime", Query.Direction.DESCENDING).startAfter(lastVisible).limit(PAGE_SIZE);
        }

        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                Log.v("TAG", "addOnSuccessListener called");
                for (QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots) {
                    PostModel postModel = documentSnapshot.toObject(PostModel.class);
                    adapter.addPost(postModel);
                    adapter.addPostId(documentSnapshot.getId());
                    Log.v("TAG", postModel.getUsername());
                }
                int currentPageSize = queryDocumentSnapshots.size();

                if (lastVisible == null) {
                    recyclerView.scrollToPosition(position);
                }

                if (currentPageSize > 0) {
                    PostViewActivity.this.lastVisible = queryDocumentSnapshots.getDocuments().get(currentPageSize - 1);
                }

                int startPosition = postModelList.size() - currentPageSize;
                int endPosition = postModelList.size() - 1;

                AndroidUtils.showToast(PostViewActivity.this, "Next Page Loaded");
                dataLoading = false;

                adapter.notifyItemRangeInserted(startPosition, endPosition);

                if (currentPageSize < PAGE_SIZE) {
                    lastItemReached = true;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (HomeFragment.fromCommentActivity) {
            HomeFragment.fromCommentActivity = false;
            FirebaseUtil.getUserPosts(userId).document(HomeFragment.postId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    try {
                        String commentCount = documentSnapshot.get("commentsCount").toString();
                        adapter.getiewHolderAtPostion(HomeFragment.commentAdapterPostionAdded).refreshCommentCount(commentCount);
                    }catch (NullPointerException e){}
                }
            });
        }
    }
}