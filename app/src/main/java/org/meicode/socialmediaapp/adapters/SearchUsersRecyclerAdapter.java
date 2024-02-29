package org.meicode.socialmediaapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.meicode.socialmediaapp.ProfileViewActivity;
import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.List;

public class SearchUsersRecyclerAdapter extends RecyclerView.Adapter<SearchUsersRecyclerAdapter.SearchUserViewHolder>{
    private Context context;
    private List<UserModel> userModelList;

    public SearchUsersRecyclerAdapter(Context context, List<UserModel> userModelList) {
        this.context = context;
        this.userModelList = userModelList;
    }

    public void addUser(UserModel userModel) {
        userModelList.add(userModel);
    }

    @NonNull
    @Override
    public SearchUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_user_recycler_row, parent, false);
        return new SearchUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchUserViewHolder holder, int position) {
        holder.profilePic.setImageResource(R.drawable.person_icon);
        UserModel model = userModelList.get(position);
        String userId = model.getUserId();

        if (!userId.equals(FirebaseUtil.getCurrentUserId())) {
            holder.usernameText.setText(model.getUsername());
        } else {
            holder.usernameText.setText(model.getUsername() + " (Me)");
        }

        FirebaseUtil.getProfilePicStorageReference(userId).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    AndroidUtils.setProfileImage(context, task.getResult(), holder.profilePic);
                }
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userId.equals(FirebaseUtil.getCurrentUserId())) {
                    AndroidUtils.showToast(context, "This is really you, have you forgotten?");
                    return;
                }
                Intent intent = new Intent(context, ProfileViewActivity.class);
                intent.putExtra("userId", userId);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userModelList.size();
    }

    class SearchUserViewHolder extends RecyclerView.ViewHolder {

        TextView usernameText;
        ImageView profilePic;

        public SearchUserViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.search_username);
            profilePic = itemView.findViewById(R.id.profile_picture_image_view);
        }
    }

}
