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

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;

import org.meicode.socialmediaapp.ChatActivity;
import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.model.ChatRoomModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

public class RecentChatAdapter extends FirestoreRecyclerAdapter<ChatRoomModel, RecentChatAdapter.RecentChatViewHolder> {

    Context context;
    public RecentChatAdapter(@NonNull FirestoreRecyclerOptions<ChatRoomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull RecentChatViewHolder holder, int position, @NonNull ChatRoomModel model) {
        FirebaseUtil.getOtherUserFromChatroom(model.getUserIds()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                holder.profilePic.setImageResource(R.drawable.person_icon);
                boolean lastMessageSentByMe = model.getLastMessageSenderId().equals(FirebaseUtil.getCurrentUserId());
                String otherUserId = documentSnapshot.getId();
                String otherUserFcmToken = (String) documentSnapshot.get("fcmToken");


                String otherUsername = documentSnapshot.getString("username").toString();

                holder.usernameTextview.setText(otherUsername);

                if (lastMessageSentByMe) holder.lastMessageText.setText("You: " + model.getLastMessage());
                else holder.lastMessageText.setText(model.getLastMessage());

                holder.lastMessageTime.setText(AndroidUtils.postedTime(model.getLastMessageTimestamp()));

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("userId", otherUserId);
                        intent.putExtra("fcmToken", otherUserFcmToken);
                        context.startActivity(intent);
                    }
                });
                FirebaseUtil.getProfilePicStorageReference(otherUserId).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        AndroidUtils.setProfileImage(context, uri, holder.profilePic);
                    }
                });
            }
        });
    }

    @NonNull
    @Override
    public RecentChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        RecentChatViewHolder recentChatViewHolder = new RecentChatViewHolder(view);
        recentChatViewHolder.usernameTextview.setText("");
        recentChatViewHolder.lastMessageText.setText("");
        recentChatViewHolder.lastMessageTime.setText("");
        recentChatViewHolder.profilePic.setImageResource(R.drawable.person_icon);

        return recentChatViewHolder;
    }

    class RecentChatViewHolder extends RecyclerView.ViewHolder{

        TextView usernameTextview;
        TextView lastMessageText;
        TextView lastMessageTime;
        ImageView profilePic;
        public RecentChatViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextview = itemView.findViewById(R.id.recent_chat_username);
            lastMessageText = itemView.findViewById(R.id.last_message_textview);
            lastMessageTime = itemView.findViewById(R.id.last_message_timestamp);
            profilePic = itemView.findViewById(R.id.recent_chat_profile_picture_image_view);
        }
    }
}
