package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONObject;
import org.meicode.socialmediaapp.adapters.ChatMessageRecyclerAdapter;
import org.meicode.socialmediaapp.fragments.HomeFragment;
import org.meicode.socialmediaapp.model.ChatMessageModel;
import org.meicode.socialmediaapp.model.ChatRoomModel;
import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;
import org.meicode.socialmediaapp.utils.PrepareUserFeed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    ChatMessageRecyclerAdapter adapter;
    RecyclerView recyclerView;
    EditText messageInputEdittext;
    ImageButton sendMessageButton;
    ImageButton backButton;
    TextView otherUsernameTextview;
    ImageView otherUserProfilePic;
    DocumentSnapshot lastVisible;
    private static final int PAGE_SIZE = 20;
    List<ChatMessageModel> messageList;
    List<ChatMessageModel> newMessageList;
    public static boolean lastItemReached;
    boolean dataLoading;
    boolean lastMessageFetched;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (getIntent().getExtras() != null && getIntent().getExtras().get("fromNotification") != null){
            SplashActivity.firstTimeLaunch = true;
            HomeFragment.fromNotification = true;
        }
    }

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
        lastItemReached = false;
        lastMessageFetched = false;
        dataLoading = false;

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        userId = getIntent().getExtras().getString("userId");
        FirebaseUtil.getUsersCollectionReference().document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                otherUserFcmToken = documentSnapshot.getString("fcmToken");
            }
        });

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
        Log.v("TAG", "GET MESSAGES CALLED");
    }

    private void setupChatRecyclerView() {
        messageList = new ArrayList<>();
        newMessageList = new ArrayList<>();

        adapter = new ChatMessageRecyclerAdapter(this, messageList, newMessageList);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();

                if (!dataLoading && (firstVisibleItem + visibleItemCount) == totalItemCount && !lastItemReached) {
                    dataLoading = true;
                    getMessages();
                }
            }
        });
    }

    private void getMessages() {
        Query query;
        if (lastVisible == null) {
            query = FirebaseUtil.getChatRoomMessageReference(chatRoomId).orderBy("timestamp", Query.Direction.DESCENDING).limit(PAGE_SIZE);
        } else {
            query = FirebaseUtil.getChatRoomMessageReference(chatRoomId).orderBy("timestamp", Query.Direction.DESCENDING).limit(PAGE_SIZE).startAfter(lastVisible);
        }
        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (!lastMessageFetched) {
                    DocumentSnapshot lastMessage = null;
                    try {
                        lastMessage = queryDocumentSnapshots.getDocuments().get(0);
                    } catch (IndexOutOfBoundsException e) {}

                    fetchNewMessages(lastMessage);
                    lastMessageFetched = true;
                }
                for (QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots) {
                    ChatMessageModel chatMessageModel = documentSnapshot.toObject(ChatMessageModel.class);
                    adapter.addMessage(chatMessageModel);
                    Log.v("TAG", chatMessageModel.getMessage());
                }

                int currentPageSize = queryDocumentSnapshots.size();


                if (currentPageSize == 0) {
                    adapter.notifyItemRemoved(adapter.getItemCount() - 1);
                    lastItemReached = true;
                    return;
                }

                Log.v("TAG", "PAGE SIZE " + currentPageSize);

                if (currentPageSize > 0) {
                    ChatActivity.this.lastVisible = queryDocumentSnapshots.getDocuments().get(currentPageSize - 1);
                }

                int startPosition = adapter.getItemCount() - currentPageSize;
                int endPosition = adapter.getItemCount() - 1;

                dataLoading = false;

                adapter.notifyItemRangeInserted(startPosition, endPosition);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dataLoading = false;
                AndroidUtils.showToast(ChatActivity.this, "Something went wrong");
            }
        });
    }

    private void fetchNewMessages(DocumentSnapshot lastMessage) {
        Query query;
        if (lastMessage != null) {
            query = FirebaseUtil.getChatRoomMessageReference(chatRoomId).orderBy("timestamp", Query.Direction.ASCENDING)
                    .startAfter(lastMessage);
        } else {
            query = FirebaseUtil.getChatRoomMessageReference(chatRoomId).orderBy("timestamp", Query.Direction.ASCENDING);
        }
        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                for (DocumentChange documentChange : value.getDocumentChanges()) {
                    ChatMessageModel chatMessageModel = documentChange.getDocument().toObject(ChatMessageModel.class);
                    adapter.addNewMessage(chatMessageModel);
                    adapter.notifyItemInserted(0);
                    recyclerView.smoothScrollToPosition(0);
                    Log.v("TAG", "NEW MESSAGE");
                }
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
}