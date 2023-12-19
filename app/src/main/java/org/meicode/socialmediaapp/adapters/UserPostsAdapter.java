package org.meicode.socialmediaapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

public class UserPostsAdapter extends FirestoreRecyclerAdapter<PostModel, UserPostsAdapter.PostUserViewHolder> {

    Context context;
    String userId;
    public PostClickListener postClickListener;

    public void setPostClickListener(PostClickListener postClickListener) {
        this.postClickListener = postClickListener;
    }

    public UserPostsAdapter(@NonNull FirestoreRecyclerOptions<PostModel> options, Context context, String userId) {
        super(options);
        this.context = context;
        this.userId = userId;
    }

    @Override
    protected void onBindViewHolder(@NonNull PostUserViewHolder holder, int position, @NonNull PostModel model) {
        String postId = getSnapshots().getSnapshot(position).getId();

        FirebaseUtil.getPostImageStorageReference(postId, userId).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    AndroidUtils.setPostImage(context, task.getResult(), holder.userPostImageview);
                }
            }
        });

    }

    @NonNull
    @Override
    public PostUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_photo_grid_layout, parent, false);
        return new PostUserViewHolder(view);
    }

    class PostUserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView userPostImageview;

        public PostUserViewHolder(@NonNull View itemView) {
            super(itemView);
            userPostImageview = itemView.findViewById(R.id.post_grid_item_imageview);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (postClickListener != null) {
                postClickListener.onPostClicked(view, getAbsoluteAdapterPosition(), userId);
            }
        }
    }
}
