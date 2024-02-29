package org.meicode.socialmediaapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;

import org.meicode.socialmediaapp.ProfileViewActivity;
import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.model.PostComments;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.List;

public class CommentsRecyclerAdapter extends RecyclerView.Adapter<CommentsRecyclerAdapter.CommentsViewHolder>{

    List<PostComments> postCommentsList;
    List<PostComments> newCommentsList;
    List<String> commentIds;
    List<String> newCommentsId;
    String userId;
    String postId;
    Context context;

    public CommentsRecyclerAdapter(List<PostComments> postCommentsList, List<String> commentIds, String postId, String userId, Context context, List<PostComments> newCommentsList, List<String> newCommentsId) {
        this.postCommentsList = postCommentsList;
        this.postId = postId;
        this.context = context;
        this.userId = userId;
        this.commentIds = commentIds;
        this.newCommentsList = newCommentsList;
        this.newCommentsId = newCommentsId;
    }

    public void addComment(PostComments postComments) {
        postCommentsList.add(postComments);
    }
    public void addCommentId(String commentId) {
        commentIds.add(commentId);
    }
    public void addNewComment(PostComments postComments) {
        newCommentsList.add(postComments);
    }
    public void addNewCommentId(String commentId) {
        newCommentsId.add(commentId);
    }
    public void removeComment(int position) {
        postCommentsList.remove(position);
        commentIds.remove(position);
    }
    public void removeNewComment(int position) {
        newCommentsList.remove(position);
        newCommentsId.remove(position);
    }

    @NonNull
    @Override
    public CommentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.comment_row, parent, false);
        return new CommentsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentsViewHolder holder, int position) {
        holder.profilePic.setImageResource(R.drawable.person_icon);

        String commentId;
        PostComments postComment;

        if (position < newCommentsList.size()) {
            commentId = newCommentsId.get(newCommentsId.size() - 1 - position);
            postComment = newCommentsList.get(newCommentsList.size() - 1 - position);
        } else {
            commentId = commentIds.get(position - newCommentsList.size());
            postComment = postCommentsList.get(position - newCommentsId.size());
        }

        FirebaseUtil.getProfilePicStorageReference(postComment.getCommentatorId()).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    AndroidUtils.setProfileImage(context, task.getResult(), holder.profilePic);
                }
            }
        });
        holder.username.setText(postComment.getCommentatorUsername());
        holder.timestamp.setText(AndroidUtils.postedTime(postComment.getCommentTimestamp()));
        holder.comment.setText(postComment.getComment());

        if (postComment.getCommentatorId().equals(FirebaseUtil.getCurrentUserId())) {
            holder.deleteComment.setVisibility(View.VISIBLE);
        } else holder.deleteComment.setVisibility(View.INVISIBLE);

        holder.deleteComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAssureDialog(userId, commentId, holder.getAbsoluteAdapterPosition());
            }
        });

        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (postComment.getCommentatorId().equals(FirebaseUtil.getCurrentUserId())) {
                    AndroidUtils.showToast(context, "This is your comment");
                    return;
                }
                Intent intent = new Intent(context, ProfileViewActivity.class);
                intent.putExtra("userId", postComment.getCommentatorId());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postCommentsList.size() + newCommentsList.size();
    }

    public class CommentsViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePic;
        TextView username;
        TextView timestamp;
        TextView comment;
        TextView deleteComment;
        public CommentsViewHolder(@NonNull View itemView) {
            super(itemView);
            profilePic = itemView.findViewById(R.id.comment_profile_picture);
            username = itemView.findViewById(R.id.comment_username);
            timestamp = itemView.findViewById(R.id.comment_timestamp);
            comment = itemView.findViewById(R.id.comment_textview);
            deleteComment = itemView.findViewById(R.id.delete_comment_textview);
        }
    }

    private void showAssureDialog(String userId, String commentId, int position) {
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
                        postCommentsList.remove(position);
                        commentIds.remove(position);
                        notifyItemRemoved(position);
                    }
                });

            alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }
}
