package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;
import org.meicode.socialmediaapp.adapters.ChatMessagesAdapter;
import org.meicode.socialmediaapp.adapters.SearchUserRecyclerAdapter;
import org.meicode.socialmediaapp.model.ChatMessageModel;
import org.meicode.socialmediaapp.model.ChatRoomModel;
import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;
import org.meicode.socialmediaapp.utils.PrepareUserFeed;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {
    String chatRoomId;
    ChatRoomModel chatRoomModel;
    String userId;
    String otherUserFcmToken;
    ChatMessagesAdapter adapter;
    RecyclerView recyclerView;
    EditText messageInputEdittext;
    ImageButton sendMessageButton;
    ImageButton backButton;
    TextView otherUsernameTextview;
    ImageView otherUserProfilePic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageInputEdittext = findViewById(R.id.chat_message_input);
        sendMessageButton = findViewById(R.id.message_send_button);
        otherUsernameTextview = findViewById(R.id.chat_user_textview);
        backButton = findViewById(R.id.go_back_button);
        recyclerView = findViewById(R.id.chat_recyclerview);
        otherUserProfilePic = findViewById(R.id.chat_profile_pic);
        ChatActivity chatActivity = this;

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getIntent().getExtras().get("getFeed") != null){
                    SplashActivity.firstTimeLaunch = true;
                    new PrepareUserFeed(chatActivity).prepareUserFeed();
                } else {
                    onBackPressed();
                }
            }
        });

        userId = getIntent().getExtras().getString("userId");
        otherUserFcmToken = getIntent().getExtras().getString("fcmToken");

        FirebaseUtil.getProfilePicStorageReference(userId).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                if (uri != null)
                    AndroidUtils.setProfileImage(getApplicationContext(), uri, otherUserProfilePic);
            }
        });

        chatRoomId = FirebaseUtil.getChatRoomId(FirebaseUtil.getCurrentUserId(), userId);

        FirebaseUtil.getUsersCollectionReference().document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String otherUsername = documentSnapshot.get("username").toString();
                otherUsernameTextview.setText(otherUsername);
            }
        });

//        FirebaseUtil.getProfilePicStorageReference(userId).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//            @Override
//            public void onSuccess(Uri uri) {
//                AndroidUtils.setProfileImage(getApplicationContext(), uri, otherUserProfilePic);
//            }
//        });

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = messageInputEdittext.getText().toString().trim();
                if (message.isEmpty()) return;
                sendMessageToUser(message);
            }
        });

        getChatRoomModel();
        setupChatRecyclerView();
    }

    private void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatRoomMessageReference(chatRoomId).orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatMessagesAdapter(options, this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    private void sendMessageToUser(String message) {
        chatRoomModel.setLastMessageTimestamp(Timestamp.now());
        chatRoomModel.setLastMessageSenderId(FirebaseUtil.getCurrentUserId());
        chatRoomModel.setLastMessage(message);
        messageInputEdittext.setText("");
        FirebaseUtil.getChatRoomReference(chatRoomId).set(chatRoomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.getCurrentUserId(), Timestamp.now());

        FirebaseUtil.getChatRoomMessageReference(chatRoomId).add(chatMessageModel).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    sendNotification(message);
                }
            }
        });
    }

    private void getChatRoomModel() {
        FirebaseUtil.getChatRoomReference(chatRoomId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    chatRoomModel = task.getResult().toObject(ChatRoomModel.class);
                    if (chatRoomModel == null) {
                        // first time chatting
                        chatRoomModel = new ChatRoomModel(chatRoomId, Arrays.asList(FirebaseUtil.getCurrentUserId(), userId), Timestamp.now(), "");
                        FirebaseUtil.getChatRoomReference(chatRoomId).set(chatRoomModel);
                    }
                }
            }
        });
    }

    private void sendNotification (String message) {
        FirebaseUtil.getCurrentUserDetails().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                try {
                    Log.v("TAG", "THIS IS THE NOTIFICATION: i tried to send it");
                    JSONObject jsonObject = new JSONObject();

                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title", documentSnapshot.get("username"));
                    notificationObj.put("body", message);

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("userId", documentSnapshot.getId());

                    jsonObject.put("notification", notificationObj);
                    jsonObject.put("data", dataObj);
                    jsonObject.put("to", otherUserFcmToken);

                    callApi(jsonObject);

                }catch (Exception e) {
                }
            }
        });
    }

    private void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer AAAA6SKAEys:APA91bEICTLx8b53XNFx9dnrqhsgwzD-ckIdp-BJJTRDtY4ZWQjCPDbOM-Vsl3iEHz90sqgeTA97SFAfax-ip8fpyAQRefJkLsa7SAs-3QZKC-yY9K28JwJ7xOvaYjoqByvwSgfegt-1")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {}
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter!= null) adapter.stopListening();
    }
}