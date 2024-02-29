package org.meicode.socialmediaapp.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.List;

public class UserPostsRecyclerAdapter extends RecyclerView.Adapter<UserPostsRecyclerAdapter.UserPostsViewHolder> {

    private Context context;
    private String userId;
    public PostClickListener postClickListener;
    private List<String> postIdList;

    public void setPostClickListener(PostClickListener postClickListener) {
        this.postClickListener = postClickListener;
    }

    public void addPostId(String postId) {
        postIdList.add(postId);
    }

    public void removeAllPosts() {
        postIdList.clear();
    }

    public UserPostsRecyclerAdapter(Context context, String userId, List<String> postIdList) {
        this.context = context;
        this.userId = userId;
        this.postIdList = postIdList;
    }

    @NonNull
    @Override
    public UserPostsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_photo_grid_layout, parent, false);
        return new UserPostsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserPostsViewHolder holder, int position) {
        String postId = postIdList.get(position);

        FirebaseUtil.getPostImageStorageReference(postId, userId).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    AndroidUtils.setPostImage(context, task.getResult(), holder.userPostImageview);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return postIdList.size();
    }

    class UserPostsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView userPostImageview;
        public UserPostsViewHolder(@NonNull View itemView) {
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
