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
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;

import org.meicode.socialmediaapp.ProfileViewActivity;
import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.model.PostComments;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

public class CommentsAdapter extends FirestoreRecyclerAdapter<PostComments, CommentsAdapter.PostCommentsViewHolder> {

    Context context;
    String postId;
    String userId;
    public CommentsAdapter(@NonNull FirestoreRecyclerOptions<PostComments> options, Context context, String postId, String userId) {
        super(options);
        this.context = context;
        this.postId = postId;
        this.userId = userId;
    }

    @Override
    protected void onBindViewHolder(@NonNull PostCommentsViewHolder holder, int position, @NonNull PostComments model) {
        String commentId = getSnapshots().getSnapshot(position).getId();

        FirebaseUtil.getProfilePicStorageReference(model.getCommentatorId()).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    AndroidUtils.setProfileImage(context, task.getResult(), holder.profilePic);
                }
            }
        });
        holder.username.setText(model.getCommentatorUsername());
        holder.timestamp.setText(AndroidUtils.postedTime(model.getCommentTimestamp()));
        holder.comment.setText(model.getComment());

        if (model.getCommentatorId().equals(FirebaseUtil.getCurrentUserId())) {
            holder.deleteComment.setVisibility(View.VISIBLE);
        } else holder.deleteComment.setVisibility(View.INVISIBLE);

        holder.deleteComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAssureDialog(userId, commentId);
            }
        });

        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.getCommentatorId().equals(FirebaseUtil.getCurrentUserId())) {
                    AndroidUtils.showToast(context, "This is your comment");
                    return;
                }
                Intent intent = new Intent(context, ProfileViewActivity.class);
                intent.putExtra("userId", model.getCommentatorId());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    @NonNull
    @Override
    public PostCommentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.comment_row, parent, false);
        return new PostCommentsViewHolder(view);
    }

    class PostCommentsViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePic;
        TextView username;
        TextView timestamp;
        TextView comment;
        TextView deleteComment;
        public PostCommentsViewHolder(@NonNull View itemView) {
            super(itemView);
            profilePic = itemView.findViewById(R.id.comment_profile_picture);
            username = itemView.findViewById(R.id.comment_username);
            timestamp = itemView.findViewById(R.id.comment_timestamp);
            comment = itemView.findViewById(R.id.comment_textview);
            deleteComment = itemView.findViewById(R.id.delete_comment_textview);
        }
    }

    private void showAssureDialog(String userId, String commentId) {
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
                FirebaseUtil.getPostComments(userId, postId).document(commentId).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FirebaseUtil.getUserPosts(userId).document(postId).update("commentsCount", FieldValue.increment(-1));

                        FirebaseUtil.getUserFeed(FirebaseUtil.getCurrentUserId()).document(postId).update("commentsCount", FieldValue.increment(-1));
                    }
                });
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }
}
