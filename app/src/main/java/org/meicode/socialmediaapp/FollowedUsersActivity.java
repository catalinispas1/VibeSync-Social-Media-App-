package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.adapters.SearchUsersRecyclerAdapter;
import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

public class FollowedUsersActivity extends AppCompatActivity {

    TextView textView;
    RecyclerView recyclerView;
    SearchUsersRecyclerAdapter adapter;
    ImageButton goBackButton;
    List<UserModel> userModelList;
    List<String> followIdList;
    private static final int PAGE_SIZE = 15;
    boolean lastItemReached;
    boolean dataLoading;
    DocumentSnapshot lastVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_followed_users);

        String userId = getIntent().getExtras().getString("userId");
        String title = getIntent().getExtras().getString("title");
        textView = findViewById(R.id.title_followed_users);
        recyclerView = findViewById(R.id.followed_users_recyclerview);
        goBackButton = findViewById(R.id.go_back_button);
        userModelList = new ArrayList<>();
        followIdList = new ArrayList<>();

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
                    getFollowUsers();
                    Log.v("TAG", "userFeedCalled because last item scroll detecrted");
                }
            }
        });

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        textView.setText(title);

        FirebaseUtil.getUsersCollectionReference().document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (title.equals("Who follows this user")) {
                    followIdList= (List<String>) documentSnapshot.get("whoFollowsMe");
                } else {
                    followIdList = (List<String>) documentSnapshot.get("whoIfollow");
                }

                adapter = new SearchUsersRecyclerAdapter(FollowedUsersActivity.this, userModelList);
                recyclerView.setLayoutManager(new LinearLayoutManager(FollowedUsersActivity.this));
                recyclerView.setAdapter(adapter);

                getFollowUsers();
            }
        });
    }

    private void getFollowUsers() {
        Query query;

        if (lastVisible == null) {
            query = FirebaseUtil.getUsersCollectionReference().whereIn("userId", followIdList).limit(PAGE_SIZE);
        } else {
            query = FirebaseUtil.getUsersCollectionReference().whereIn("userId", followIdList).limit(PAGE_SIZE).startAfter(lastVisible);
        }
        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots) {
                    UserModel userModel = documentSnapshot.toObject(UserModel.class);
                    adapter.addUser(userModel);
                    Log.v("TAG", "QUERY GET CALLED");
                    Log.v("TAG", userModel.getUsername());
                }

                int currentPageSize = queryDocumentSnapshots.size();
                if (currentPageSize > 0) {
                    lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                }

                if (currentPageSize < PAGE_SIZE) {
                    lastItemReached = true;
                }

                int startPosition = userModelList.size() - currentPageSize;
                int endPosition = userModelList.size() - 1;

                if (startPosition == endPosition) {
                    adapter.notifyItemInserted(startPosition);
                } else {
                    adapter.notifyItemRangeInserted(startPosition, endPosition);
                }
                dataLoading = false;
            }
        });
    }
}