package org.meicode.socialmediaapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.meicode.socialmediaapp.ChatActivity;
import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.model.ChatMessageModel;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.List;

public class ChatMessageRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private Context context;
    private List<ChatMessageModel> chatMessageModelList;
    private List<ChatMessageModel> newChatMessageModelList;
    private static final int VIEW_TYPE_MESSAGE = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    public ChatMessageRecyclerAdapter(Context context, List<ChatMessageModel> chatMessageModelList, List<ChatMessageModel> newChatMessageModelList) {
        this.context = context;
        this.chatMessageModelList = chatMessageModelList;
        this.newChatMessageModelList = newChatMessageModelList;
    }

    public void addMessage(ChatMessageModel chatMessageModel) {
        chatMessageModelList.add(chatMessageModel);
    }
    public void addNewMessage(ChatMessageModel chatMessageModel) {
        newChatMessageModelList.add(chatMessageModel);
    }

    @Override
    public int getItemViewType(int position) {
        if (position < chatMessageModelList.size() + newChatMessageModelList.size()) {
            return VIEW_TYPE_MESSAGE;
        } else {
            return VIEW_TYPE_LOADING;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view;
        if (viewType == VIEW_TYPE_MESSAGE) {
            view = inflater.inflate(R.layout.chat_message_recycler_row, parent, false);
            ChatMessageViewHolder viewHolder = new ChatMessageViewHolder(view);
            viewHolder.leftChatLayout.setVisibility(View.GONE);
            viewHolder.rightChatLayout.setVisibility(View.GONE);
            return viewHolder;
        } else if (viewType == VIEW_TYPE_LOADING) {
            view = inflater.inflate(R.layout.loading_row, parent, false);
            LoadingViewHolder viewHolder = new LoadingViewHolder(view);
            return viewHolder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ChatMessageViewHolder) {
            ChatMessageModel messageModel;
            if (position < newChatMessageModelList.size()) {
                messageModel = newChatMessageModelList.get(newChatMessageModelList.size() - 1 - position);
            } else {
                messageModel = chatMessageModelList.get(position - newChatMessageModelList.size());
            }
            ChatMessageViewHolder messageViewHolder = (ChatMessageViewHolder) holder;

            if (messageModel.getSenderId().equals(FirebaseUtil.getCurrentUserId())) {
                messageViewHolder.leftChatLayout.setVisibility(View.GONE);
                messageViewHolder.rightChatLayout.setVisibility(View.VISIBLE);
                messageViewHolder.rightChatTextview.setText(messageModel.getMessage());
            } else {
                messageViewHolder.rightChatLayout.setVisibility(View.GONE);
                messageViewHolder.leftChatLayout.setVisibility(View.VISIBLE);
                messageViewHolder.leftChatTextview.setText(messageModel.getMessage());
            }
        }
    }

    @Override
    public int getItemCount() {
        if (!ChatActivity.lastItemReached) {
            return chatMessageModelList.size() + newChatMessageModelList.size() + 1;
        } else {
            return chatMessageModelList.size() + newChatMessageModelList.size();
        }
    }

    class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextview, rightChatTextview;
        public ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
        }
    }

    class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(View itemView) {
            super(itemView);
        }
    }
}
