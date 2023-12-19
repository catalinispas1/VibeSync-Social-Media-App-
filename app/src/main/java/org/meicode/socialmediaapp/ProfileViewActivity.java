package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.adapters.PostClickListener;
import org.meicode.socialmediaapp.adapters.UserPostsAdapter;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.List;

public class ProfileViewActivity extends AppCompatActivity implements PostClickListener {

    UserModel userProfile;
    String userId;
    TextView usernameTextview;
    TextView postCountTextview;
    TextView whoIfollowTextview;
    TextView whoFollowsMeTextview;
    TextView bioTextview;
    ImageView profilePic;
    ImageButton goBackButton;
    RecyclerView recyclerView;
    Button followButton;
    Button unfollowButton;
    Button messageButton;
    boolean userInitiallyFollowed;
    UserPostsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);

        usernameTextview = findViewById(R.id.username_profile);
        postCountTextview = findViewById(R.id.post_count);
        whoIfollowTextview = findViewById(R.id.who_i_follow);
        whoFollowsMeTextview = findViewById(R.id.who_follows_me_count);
        bioTextview = findViewById(R.id.bio_text);
        profilePic = findViewById(R.id.profile_picture);
        goBackButton = findViewById(R.id.back_btn);
        recyclerView = findViewById(R.id.user_posts_recyclerview);
        followButton = findViewById(R.id.follow_button);
        unfollowButton = findViewById(R.id.unfollow_button);
        messageButton = findViewById(R.id.message_button);

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        userId = getIntent().getExtras().getString("userId");

        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileViewActivity.this, ChatActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        whoFollowsMeTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (whoFollowsMeTextview.getText().toString().equals("0\nfollows")) {
                    AndroidUtils.showToast(getApplicationContext(), "This user has 0 follows");
                    return;
                }
                Intent intent = new Intent(ProfileViewActivity.this, FollowedUsersActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("title", "Who follows this user");
                startActivity(intent);
            }
        });

        whoIfollowTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (whoIfollowTextview.getText().toString().equals("0\nfollowing")) {
                    AndroidUtils.showToast(getApplicationContext(), "This user is following nobody");
                    return;
                }
                Intent intent = new Intent(ProfileViewActivity.this, FollowedUsersActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("title", "Who is following this user");
                startActivity(intent);
            }
        });

        FirebaseUtil.getUsersCollectionReference().document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    userProfile = task.getResult().toObject(UserModel.class);
                    if (userProfile.getWhoFollowsMe().contains(FirebaseUtil.getCurrentUserId())) {
                        followButton.setVisibility(View.GONE);
                        userInitiallyFollowed = true;
                    } else {
                        unfollowButton.setVisibility(View.GONE);
                    }

                    whoFollowsMeTextview.setText(userProfile.getWhoFollowsMe().size() + "\nfollows");
                    whoIfollowTextview.setText(userProfile.getWhoIfollow().size() + "\nfollowing");
                    usernameTextview.setText(userProfile.getUsername());
                    bioTextview.setText(userProfile.getBio());
                }
            }
        });

        FirebaseUtil.getUserPosts(userId).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    postCountTextview.setText(task.getResult().size() + "\nposts");
                }
            }
        });

        FirebaseUtil.getProfilePicStorageReference(userId).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    AndroidUtils.setProfileImage(getApplicationContext(), task.getResult(), profilePic);
                }
            }
        });

        Query query = FirebaseUtil.getUserPosts(userId).orderBy("uploadTime", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<PostModel> options = new FirestoreRecyclerOptions.Builder<PostModel>()
                .setQuery(query, PostModel.class).build();

        adapter = new UserPostsAdapter(options, getApplicationContext(), userId);
        recyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 3));

        recyclerView.setAdapter(adapter);
        adapter.setPostClickListener(this);
        adapter.startListening();

        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addOrRemoveFollowers(true);
            }
        });

        unfollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addOrRemoveFollowers(false);
            }
        });
    }

    private void addOrRemoveFollowers(boolean addFollower) {
        if (addFollower) {
            followButton.setVisibility(View.GONE);
            //userProfile.getWhoFollowsMe().add(FirebaseUtil.getCurrentUserId());
            FirebaseUtil.getUsersCollectionReference().document(userId)
                    .update("whoFollowsMe", FieldValue.arrayUnion(FirebaseUtil.getCurrentUserId()))
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            unfollowButton.setVisibility(View.VISIBLE);
                            if (!userInitiallyFollowed) {
                                whoFollowsMeTextview.setText(userProfile.getWhoFollowsMe().size() + 1 + "\nfollows");
                            } else {
                                whoFollowsMeTextview.setText(userProfile.getWhoFollowsMe().size() + "\nfollows");
                            }
                            FirebaseUtil.getCurrentUserDetails().update("whoIfollow", FieldValue.arrayUnion(userId));
                            FirebaseUtil.getUserPosts(userId).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot userPosts: queryDocumentSnapshots) {
                                        FirebaseUtil.getUserPosts(userId).document(userPosts.getId())
                                                .update("userThatWillSeePostOnFeed", FieldValue.arrayUnion(FirebaseUtil.getCurrentUserId()));
                                    }
                                }
                            });
                        }
                    });
        } else {
            unfollowButton.setVisibility(View.GONE);
            //userProfile.getWhoFollowsMe().remove(FirebaseUtil.getCurrentUserId());
            FirebaseUtil.getUsersCollectionReference().document(userId)
                    .update("whoFollowsMe", FieldValue.arrayRemove(FirebaseUtil.getCurrentUserId()))
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            followButton.setVisibility(View.VISIBLE);

                            if (userInitiallyFollowed) {
                                whoFollowsMeTextview.setText(userProfile.getWhoFollowsMe().size() - 1 + "\nfollows");
                            } else {
                                whoFollowsMeTextview.setText(userProfile.getWhoFollowsMe().size() + "\nfollows");
                            }
                            FirebaseUtil.getCurrentUserDetails().update("whoIfollow", FieldValue.arrayRemove(userId));
                            FirebaseUtil.getUserPosts(userId).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot userPosts: queryDocumentSnapshots) {
                                        FirebaseUtil.getUserPosts(userId).document(userPosts.getId())
                                                .update("userThatWillSeePostOnFeed", FieldValue.arrayRemove(FirebaseUtil.getCurrentUserId()));
                                    }
                                }
                            });
                        }
                    });
        }
    }

    @Override
    public void onPostClicked(View view, int position, String userId) {
        Intent intent = new Intent(ProfileViewActivity.this, PostViewActivity.class);
        intent.putExtra("position", position);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
}