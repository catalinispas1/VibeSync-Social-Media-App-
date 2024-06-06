package org.meicode.socialmediaapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.SplashActivity;
import org.meicode.socialmediaapp.adapters.FeedPostsRecyclerAdaper;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.utils.FirebaseUtil;
import org.meicode.socialmediaapp.utils.PrepareUserFeed;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    RecyclerView feedRecyclerView;
    FeedPostsRecyclerAdaper adapter;
    List<PostModel> postModelList;
    List<String> postsIds;
    TextView emptyFeedTextview;
    DocumentSnapshot lastVisible;
    private static final int PAGE_SIZE = 3;

    public static boolean lastItemReached;
    boolean dataLoading;
    ImageButton refreshButton;
    public static boolean fromNotification;
    public static boolean fromCommentActivity;
    public static int commentAdapterPostionAdded;
    public static String userId;
    public static String postId;

    public HomeFragment() {
        // Required empty public constructor
    }

    public void setRefreshButton (ImageButton refreshButton) {
        this.refreshButton = refreshButton;
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchNewPosts();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        feedRecyclerView = view.findViewById(R.id.feed_recyclerview);
        emptyFeedTextview = view.findViewById(R.id.empty_feed_textview);
        if (!SplashActivity.firstTimeLaunch) {
            instantiateAdapter();
            getUserFeed();
        }

        return view;
    }
    private void fetchNewPosts() {
        PrepareUserFeed prepareUserFeed = new PrepareUserFeed(lastVisible, adapter,  PAGE_SIZE, getContext(), fromNotification);
        prepareUserFeed.prepareUserFeed();
    }
    private void instantiateAdapter() {
        lastItemReached = false;
        dataLoading = false;
        lastVisible = null;

        postModelList = new ArrayList<>();
        postsIds = new ArrayList<>();
        adapter = new FeedPostsRecyclerAdaper(postModelList, postsIds, getContext(), feedRecyclerView);
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        feedRecyclerView.setAdapter(adapter);
        feedRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) feedRecyclerView.getLayoutManager();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();

                if (!dataLoading && (firstVisibleItem + visibleItemCount) == totalItemCount && !lastItemReached) {
                    dataLoading = true;
                    getUserFeed();
                    Log.v("TAG", "userFeedCalled because last item scroll detecrted");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fromNotification) {
            instantiateAdapter();
            fetchNewPosts();
            fromNotification = false;
            return;
        }
        if (SplashActivity.firstTimeLaunch) {
            instantiateAdapter();
            getUserFeed();
            SplashActivity.firstTimeLaunch = false;
        }
        if (fromCommentActivity) {
            fromCommentActivity = false;
            FirebaseUtil.getUserPosts(userId).document(postId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    try {
                        String commentCount = documentSnapshot.get("commentsCount").toString();
                        adapter.getiewHolderAtPostion(commentAdapterPostionAdded).refreshCommentCount(commentCount);
                    }catch (Exception e){}
                }
            });
        }
    }

    private void getUserFeed() {
        Query query;

        if (lastVisible == null) {
            query = FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId())
                .orderBy("uploadTime", Query.Direction.DESCENDING).limit(PAGE_SIZE);
        } else {
            query = FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId())
                    .orderBy("uploadTime", Query.Direction.DESCENDING).limit(PAGE_SIZE).startAfter(lastVisible);
        }

        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                Log.v("TAG", "addOnSuccessListener called");
                for (QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots) {
                    PostModel postModel = documentSnapshot.toObject(PostModel.class);
                    adapter.addPost(postModel);
                    Log.v("TAG", postModel.getUsername() + " " + postModel.getCommentsCount());
                    adapter.addPostId(documentSnapshot.getId());
                }
                int currentPageSize = queryDocumentSnapshots.size();

                if (currentPageSize > 0) {
                    HomeFragment.this.lastVisible = queryDocumentSnapshots.getDocuments().get(currentPageSize - 1);
                }

                int startPosition = postModelList.size() - currentPageSize;
                int endPosition = postModelList.size() - 1;

                dataLoading = false;

                adapter.notifyItemRangeInserted(startPosition, endPosition);

                if (currentPageSize < PAGE_SIZE) {
                    lastItemReached = true;
                }
            }
        });
    }
}