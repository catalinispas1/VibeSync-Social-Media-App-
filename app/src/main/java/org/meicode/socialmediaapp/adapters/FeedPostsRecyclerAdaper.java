package org.meicode.socialmediaapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import org.meicode.socialmediaapp.CommentsActivity;
import org.meicode.socialmediaapp.ProfileViewActivity;
import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.fragments.HomeFragment;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

public class FeedPostsRecyclerAdaper extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<PostModel> postModelList;
    private List<String> postIds;
    private Context context;
    private RecyclerView recyclerView;
    Animation scaleUp;

    public FeedPostsRecyclerAdaper(List<PostModel> postModelList, List<String> postIds, Context context, RecyclerView recyclerView) {
        this.postModelList = postModelList;
        this.context = context;
        this.postIds = postIds;
        this.recyclerView = recyclerView;
        scaleUp = AnimationUtils.loadAnimation(context, R.anim.scale_up);
    }

    public void addPost(PostModel postModel) {
        postModelList.add(postModel);
    }
    public void addPostId(String postId) {
        postIds.add(postId);
    }
    public void removePost(int position) {
        postModelList.remove(position);
    }
    public void removePostId(int position) {
        postIds.remove(position);
    }
    public void removeAllPosts() {
        postModelList.clear();
        postIds.clear();
    }
    public void setLikeAndCommentCount(List<String> likesList, int comments, int position) {
        PostModel postModel = postModelList.get(position);
        postModel.setUserThatLiked(likesList);
        postModel.setCommentsCount(comments);
        postModelList.set(position, postModel);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.post_user_recycler_row, parent, false);
        return new FeedPostsRecyclerAdaper.PostModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PostModelViewHolder postModelViewHolder = (PostModelViewHolder) holder;
        String postId = postIds.get(position);
        PostModel currentPostModel = postModelList.get(position);
        postModelViewHolder.likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId()).document(postId)
                        .update("userThatLiked", FieldValue.arrayUnion(FirebaseUtil.getCurrentUserId()));

                postModelViewHolder.likeButton.setVisibility(View.INVISIBLE);
                postModelViewHolder.unlikeButton.setVisibility(View.VISIBLE);
                postModelViewHolder.unlikeButton.startAnimation(scaleUp);

                FirebaseUtil.getUserPosts(currentPostModel.getUserId()).document(postId)
                        .update("userThatLiked", FieldValue.arrayUnion(FirebaseUtil.getCurrentUserId()));

                String[] likeCount = postModelViewHolder.likesCountTextview.getText().toString().split(" ");
                int newLikeCount = Integer.parseInt(likeCount[0]) + 1;
                postModelViewHolder.likesCountTextview.setText(newLikeCount + " " + likeCount[1]);
                currentPostModel.addUserThatLiked(FirebaseUtil.getCurrentUserId());
            }
        });

        postModelViewHolder.unlikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId()).document(postId)
                        .update("userThatLiked", FieldValue.arrayRemove(FirebaseUtil.getCurrentUserId()));

                postModelViewHolder.likeButton.setVisibility(View.VISIBLE);
                postModelViewHolder.unlikeButton.setVisibility(View.INVISIBLE);
                postModelViewHolder.likeButton.startAnimation(scaleUp);
                FirebaseUtil.getUserPosts(currentPostModel.getUserId()).document(postId)
                        .update("userThatLiked", FieldValue.arrayRemove(FirebaseUtil.getCurrentUserId()));

                String[] likeCount = postModelViewHolder.likesCountTextview.getText().toString().split(" ");
                int newLikeCount = Integer.parseInt(likeCount[0]) - 1;
                postModelViewHolder.likesCountTextview.setText(newLikeCount + " " + likeCount[1]);
                currentPostModel.removeUserThatLiked(FirebaseUtil.getCurrentUserId());
            }
        });

        postModelViewHolder.profilePic.setImageResource(R.drawable.person_icon);
        postModelViewHolder.username.setText("");
        postModelViewHolder.postImage.setImageResource(R.drawable.loading_image_icon);
        postModelViewHolder.postTimestamp.setText("");
        postModelViewHolder.postDescription.setText("");
        postModelViewHolder.likesCountTextview.setText("");
        postModelViewHolder.commentsCountTextview.setText("");

        postModelViewHolder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!currentPostModel.getUserId().equals(FirebaseUtil.getCurrentUserId())) {
                    Intent intent = new Intent(context, ProfileViewActivity.class);
                    intent.putExtra("userId", currentPostModel.getUserId());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        });

        if (currentPostModel.getUserThatLiked().contains(FirebaseUtil.getCurrentUserId())) {
            postModelViewHolder.likeButton.setVisibility(View.INVISIBLE);
            postModelViewHolder.unlikeButton.setVisibility(View.VISIBLE);
        } else {
            postModelViewHolder.unlikeButton.setVisibility(View.INVISIBLE);
            postModelViewHolder.likeButton.setVisibility(View.VISIBLE);
        }

        FirebaseUtil.getProfilePicStorageReference(currentPostModel.getUserId()).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    AndroidUtils.setProfileImage(context, task.getResult(), postModelViewHolder.profilePic);
                }
            }
        });

        FirebaseUtil.getPostImageStorageReference(postId, currentPostModel.getUserId()).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    AndroidUtils.setPostImage(context, task.getResult(), postModelViewHolder.postImage);
                }
            }
        });

        postModelViewHolder.postTimestamp.setText(AndroidUtils.postedTime(currentPostModel.getUploadTime()));

        if (!currentPostModel.getPostDescription().isEmpty()) {
            postModelViewHolder.postDescription.setVisibility(View.VISIBLE);
            postModelViewHolder.postDescription.setText(currentPostModel.getPostDescription());
        } else postModelViewHolder.postDescription.setVisibility(View.GONE);

        postModelViewHolder.likesCountTextview.setText(currentPostModel.getUserThatLiked().size() + " likes");

        postModelViewHolder.commentsCountTextview.setText(currentPostModel.getCommentsCount() + " comments");
        postModelViewHolder.username.setText(currentPostModel.getUsername());

        postModelViewHolder.commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CommentsActivity.class);

                intent.putExtra("userId", currentPostModel.getUserId());
                intent.putExtra("postId", postId);
                HomeFragment.commentAdapterPostionAdded = holder.getAbsoluteAdapterPosition();

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        if (currentPostModel.getUserId().equals(FirebaseUtil.getCurrentUserId())) {
            postModelViewHolder.deletePostTextview.setVisibility(View.VISIBLE);
        } else postModelViewHolder.deletePostTextview.setVisibility(View.INVISIBLE);

        postModelViewHolder.deletePostTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAssureDialog(currentPostModel.getUserId(), postId, postModelViewHolder.getAbsoluteAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return postModelList.size();
    }

    public class PostModelViewHolder extends RecyclerView.ViewHolder {
        ImageView postImage; //
        ImageView profilePic; //
        Button likeButton; //
        Button unlikeButton;
        Button commentButton; //
        TextView username; //
        TextView likesCountTextview; //
        TextView commentsCountTextview; //
        TextView postTimestamp; //
        TextView postDescription; //
        TextView deletePostTextview; //

        public PostModelViewHolder(@NonNull View itemView) {
            super(itemView);
            postImage = itemView.findViewById(R.id.post_picture_imageview);
            profilePic = itemView.findViewById(R.id.username_profile_picture_feed);
            likeButton = itemView.findViewById(R.id.like_button);
            unlikeButton = itemView.findViewById(R.id.dislike_button);
            commentButton = itemView.findViewById(R.id.comment_button);
            username = itemView.findViewById(R.id.post_username_feed);
            likesCountTextview = itemView.findViewById(R.id.likes_count_textview);
            commentsCountTextview = itemView.findViewById(R.id.comments_count_textview);
            postTimestamp = itemView.findViewById(R.id.timestamp_post);
            postDescription = itemView.findViewById(R.id.description_textview);
            deletePostTextview = itemView.findViewById(R.id.delete_post_textview);
        }

        public void refreshCommentCount(String commentCount) {
            commentsCountTextview.setText(commentCount + " comments");
            postModelList.get(getAbsoluteAdapterPosition()).setCommentsCount(Integer.parseInt(commentCount));
        }
    }

    public PostModelViewHolder getiewHolderAtPostion (int position) {
        PostModelViewHolder viewHolder = (PostModelViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
        return viewHolder;
    }

    private void showAssureDialog(String userId, String postId, int position) {
        View view = LayoutInflater.from(context).inflate(R.layout.assure_dialog, null);
        Button cancelButton = view.findViewById(R.id.cancel_button);
        Button deleteButton = view.findViewById(R.id.delete_button);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        AlertDialog alertDialog = builder.create();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FirebaseUtil.getUserPosts(userId).document(postId).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            FirebaseUtil.getPostImageStorageReference(postId, userId).delete();
                            FirebaseUtil.getUsersCollectionReference().document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    List<String> whoFollowsMe = (List<String>) documentSnapshot.get("whoFollowsMe");

                                    for (String userThatFollowsMe: whoFollowsMe) {
                                        FirebaseUtil.getUserFeed(userThatFollowsMe).document(postId).delete();
                                    }
                                    postModelList.remove(position);
                                    postIds.remove(position);
                                    notifyItemRemoved(position);
                                }
                            });
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }
}
