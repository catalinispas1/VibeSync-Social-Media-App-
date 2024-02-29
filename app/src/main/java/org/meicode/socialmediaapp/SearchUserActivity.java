package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.adapters.SearchUsersRecyclerAdapter;
import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

public class SearchUserActivity extends AppCompatActivity {

    EditText searchInput;
    ImageButton searchButton;
    ImageButton backButton;
    RecyclerView recyclerView;
    String searchedUsername;
    ProgressBar progressBar;
    SearchUsersRecyclerAdapter adapter;
    private static final int PAGE_SIZE = 15;
    List<UserModel> userModelList;
    boolean lastItemReached;
    boolean dataLoading;
    DocumentSnapshot lastVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        searchInput = findViewById(R.id.search_user_name_input);
        searchButton = findViewById(R.id.search_user_btn);
        backButton = findViewById(R.id.back_btn);
        recyclerView = findViewById(R.id.search_user_recycler_view);
        progressBar = findViewById(R.id.search_user_loading);

        userModelList = new ArrayList<>();

        searchInput.requestFocus();
        lastVisible = null;

        adapter = new SearchUsersRecyclerAdapter(this, userModelList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
                    searchUsers(searchedUsername);
                    Log.v("TAG", "userFeedCalled because last item scroll detecrted");
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchedUsername = searchInput.getText().toString();
                if (searchedUsername.isEmpty() || searchedUsername.length() < 4) {
                    searchInput.setError("Invalid Username");
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                searchButton.setVisibility(View.INVISIBLE);

                userModelList.clear();
                adapter.notifyDataSetChanged();
                lastItemReached = false;
                lastVisible = null;
                searchUsers(searchedUsername);
            }
        });
    }

    private void searchUsers(String searchedUsername) {
        Query query;
        if (lastVisible == null) {
            query = FirebaseUtil.getUsersCollectionReference()
                    .whereGreaterThanOrEqualTo("username", searchedUsername)
                    .whereLessThanOrEqualTo("username", searchedUsername + '\uf8ff').limit(PAGE_SIZE);
        } else {
            query = FirebaseUtil.getUsersCollectionReference()
                    .whereGreaterThanOrEqualTo("username", searchedUsername)
                    .whereLessThanOrEqualTo("username", searchedUsername + '\uf8ff').limit(PAGE_SIZE)
                    .startAfter(lastVisible);
        }

        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (lastVisible == null) {
                    progressBar.setVisibility(View.INVISIBLE);
                    searchButton.setVisibility(View.VISIBLE);
                }
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
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (lastVisible == null) {
                    progressBar.setVisibility(View.VISIBLE);
                    searchButton.setVisibility(View.INVISIBLE);
                }
                AndroidUtils.showToast(getApplicationContext(), "Something went wrong");
            }
        });
    }
}