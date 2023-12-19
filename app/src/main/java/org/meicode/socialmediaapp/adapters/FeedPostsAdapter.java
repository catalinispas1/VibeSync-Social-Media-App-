package org.meicode.socialmediaapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import org.meicode.socialmediaapp.CommentsActivity;
import org.meicode.socialmediaapp.ProfileViewActivity;
import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class FeedPostsAdapter extends FirestoreRecyclerAdapter<PostModel, FeedPostsAdapter.PostModelViewHolder> {

    Context context;

    public FeedPostsAdapter(@NonNull FirestoreRecyclerOptions<PostModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull PostModelViewHolder holder, int position, @NonNull PostModel model) {
        String postId = getSnapshots().getSnapshot(position).getId();

        holder.likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId()).document(postId)
                        .update("userThatLiked", FieldValue.arrayUnion(FirebaseUtil.getCurrentUserId()));
                holder.likeButton.setVisibility(View.INVISIBLE);
                holder.unlikeButton.setVisibility(View.VISIBLE);
                FirebaseUtil.getUserPosts(model.getUserId()).document(postId)
                        .update("userThatLiked", FieldValue.arrayUnion(FirebaseUtil.getCurrentUserId()));
            }
        });

        holder.unlikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId()).document(postId)
                        .update("userThatLiked", FieldValue.arrayRemove(FirebaseUtil.getCurrentUserId()));

                holder.likeButton.setVisibility(View.VISIBLE);
                holder.unlikeButton.setVisibility(View.INVISIBLE);
                FirebaseUtil.getUserPosts(model.getUserId()).document(postId)
                        .update("userThatLiked", FieldValue.arrayRemove(FirebaseUtil.getCurrentUserId()));
            }
        });

        holder.profilePic.setImageResource(R.drawable.person_icon);
        holder.username.setText("");
        holder.postImage.setImageResource(R.drawable.loading_image_icon);
        holder.postTimestamp.setText("");
        holder.postDescription.setText("");
        holder.likesCountTextview.setText("");
        holder.commentsCountTextview.setText("");

        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!model.getUserId().equals(FirebaseUtil.getCurrentUserId())) {
                    Intent intent = new Intent(context, ProfileViewActivity.class);
                    intent.putExtra("userId", model.getUserId());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        });

        if (model.getUserThatLiked().contains(FirebaseUtil.getCurrentUserId())) {
            holder.likeButton.setVisibility(View.INVISIBLE);
            holder.unlikeButton.setVisibility(View.VISIBLE);
        } else {
            holder.unlikeButton.setVisibility(View.INVISIBLE);
            holder.likeButton.setVisibility(View.VISIBLE);
        }

        FirebaseUtil.getProfilePicStorageReference(model.getUserId()).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    AndroidUtils.setProfileImage(context, task.getResult(), holder.profilePic);
                }
            }
        });

        FirebaseUtil.getPostImageStorageReference(postId, model.getUserId()).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    AndroidUtils.setPostImage(context, task.getResult(), holder.postImage);
                }
            }
        });

        holder.postTimestamp.setText(AndroidUtils.postedTime(model.getUploadTime()));

        if (!model.getPostDescription().isEmpty()) {
            holder.postDescription.setText(model.getPostDescription());
        } else holder.postDescription.setVisibility(View.GONE);


        holder.likesCountTextview.setText(model.getUserThatLiked().size() + " likes");

        holder.commentsCountTextview.setText(model.getCommentsCount() + " comments");
        holder.username.setText(model.getUsername());


        holder.commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CommentsActivity.class);

                intent.putExtra("userId", model.getUserId());
                intent.putExtra("postId", postId);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        if (model.getUserId().equals(FirebaseUtil.getCurrentUserId())) {
            holder.deletePostTextview.setVisibility(View.VISIBLE);
        } else holder.deletePostTextview.setVisibility(View.INVISIBLE);

        holder.deletePostTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAssureDialog(model.getUserId(), postId);
            }
        });
    }

    @NonNull
    @Override
    public PostModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.post_user_recycler_row, parent, false);
        return new PostModelViewHolder(view);
    }

    class PostModelViewHolder extends RecyclerView.ViewHolder{
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
    }

    private void showAssureDialog(String userId, String postId) {
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