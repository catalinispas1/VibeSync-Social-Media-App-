package org.meicode.socialmediaapp.utils;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.MainActivity;
import org.meicode.socialmediaapp.SplashActivity;
import org.meicode.socialmediaapp.model.PostModel;

import java.util.List;

public class PrepareUserFeed {
    int postQueriesCount;
    String lastUserFollowed;
    boolean foundUserPosts;
    int countUsersWithoutPosts;
    Activity activity;

    public PrepareUserFeed(Activity context) {
        this.activity = context;
    }

    public PrepareUserFeed() {
    }

    public void prepareUserFeed() {
        FirebaseUtil.getCurrentUserDetails().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    List<String> whoIfollowList = (List<String>) documentSnapshot.get("whoIfollow");
                    if (whoIfollowList.isEmpty() && activity != null) {
                        startMainActivity();
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
                                                                                if (postQueriesCount == postsSize && activity != null) {
                                                                                    startMainActivity();
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
                                            if (countUsersWithoutPosts == whoIfollowList.size() && activity != null) {
                                                startMainActivity();
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
}
