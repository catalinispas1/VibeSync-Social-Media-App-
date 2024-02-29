package org.meicode.socialmediaapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.MainActivity;
import org.meicode.socialmediaapp.SplashActivity;
import org.meicode.socialmediaapp.adapters.FeedPostsRecyclerAdaper;
import org.meicode.socialmediaapp.fragments.HomeFragment;
import org.meicode.socialmediaapp.model.PostModel;

import java.util.List;

public class PrepareUserFeed {
    int postQueriesCount;
    String lastUserFollowed;
    boolean foundUserPosts;
    int countUsersWithoutPosts;
    Activity activity;

    DocumentSnapshot lastVisible;
    FeedPostsRecyclerAdaper adapter;
    int passedPageSize;
    Context context;
    boolean fromNotification;

    public PrepareUserFeed(Activity activity) {
        this.activity = activity;
    }

    public PrepareUserFeed(DocumentSnapshot lastVisible, FeedPostsRecyclerAdaper adapter, int passedPageSize, Context context, boolean fromNotification) {
        this.lastVisible = lastVisible;
        this.adapter = adapter;
        this.passedPageSize = passedPageSize;
        this.context = context;
        this.fromNotification = fromNotification;
    }

    public void prepareUserFeed() {
        FirebaseUtil.getCurrentUserDetails().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    List<String> whoIfollowList = (List<String>) documentSnapshot.get("whoIfollow");

                    if (whoIfollowList.isEmpty()) {
                        if (activity != null) {
                            startMainActivity();
                        } else {
                            AndroidUtils.showToast(context, "Start following someone");
                        }
                        return;
                    }
                    for (int i = whoIfollowList.size() - 1; i >= 0 && !foundUserPosts; i--) {
                        final int index = i;
                        FirebaseUtil.getUserPosts(whoIfollowList.get(i)).whereArrayContains("userThatWillSeePostOnFeed", FirebaseUtil.getCurrentUserId())
                                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        if (!queryDocumentSnapshots.isEmpty()) {
                                            lastUserFollowed = whoIfollowList.get(index);
                                            foundUserPosts = true;

                                            for (String followedUserId : whoIfollowList) {

                                                FirebaseUtil.getUserPosts(followedUserId)
                                                        .whereArrayContains("userThatWillSeePostOnFeed", FirebaseUtil.getCurrentUserId())
                                                        .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                                final int postsSize = queryDocumentSnapshots.size();
                                                                for (QueryDocumentSnapshot postDocument: queryDocumentSnapshots) {
                                                                    FirebaseUtil.getUserPosts(followedUserId).document(postDocument.getId())
                                                                            .update("userThatWillSeePostOnFeed", FieldValue.arrayRemove(FirebaseUtil.getCurrentUserId()));
                                                                    PostModel postModel = postDocument.toObject(PostModel.class);

                                                                    FirebaseUtil.setUserFeed(FirebaseUtil.getCurrentUserId(), postDocument.getId()).set(postModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {
                                                                            if (followedUserId.equals(lastUserFollowed)) {
                                                                                postQueriesCount++;
                                                                                if (postQueriesCount == postsSize) {
                                                                                    if (activity != null) {
                                                                                        startMainActivity();
                                                                                    } else {
                                                                                        refreshFeed();
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        });
                                            }
                                        } else {
                                            countUsersWithoutPosts++;
                                            if (countUsersWithoutPosts == whoIfollowList.size()) {
                                                if (activity != null) {
                                                    startMainActivity();
                                                } else {
                                                    if (fromNotification) {
                                                        refreshFeed();
                                                    } else AndroidUtils.showToast(context, "There are no new posts");
                                                }
                                            }
                                        }
                                    }
                                });
                    }
                }
            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private void refreshFeed() {
        adapter.removeAllPosts();
        adapter.notifyDataSetChanged();
        lastVisible = null;
        HomeFragment.lastItemReached = false;

        Query query = FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId())
                .orderBy("uploadTime", Query.Direction.DESCENDING).limit(passedPageSize);
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

                if (currentPageSize > 0) {
                    lastVisible = queryDocumentSnapshots.getDocuments().get(currentPageSize - 1);
                }

                adapter.notifyDataSetChanged();

                if (currentPageSize < passedPageSize) {
                    HomeFragment.lastItemReached = true;
                }
            }
        });
    }
}
