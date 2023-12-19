package org.meicode.socialmediaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.adapters.SearchUserRecyclerAdapter;
import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.List;

public class FollowedUsersActivity extends AppCompatActivity {

    TextView textView;
    RecyclerView recyclerView;
    SearchUserRecyclerAdapter adapter;
    ImageButton goBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_followed_users);

        String userId = getIntent().getExtras().getString("userId");
        String title = getIntent().getExtras().getString("title");
        textView = findViewById(R.id.title_followed_users);
        recyclerView = findViewById(R.id.followed_users_recyclerview);
        goBackButton = findViewById(R.id.go_back_button);

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
                Query query;
                if (title.equals("Who follows this user")) {
                    List<String> whoFollowsMe = (List<String>) documentSnapshot.get("whoFollowsMe");
                    query = FirebaseUtil.getUsersCollectionReference().whereIn("userId", whoFollowsMe);
                } else {
                    List<String> whoIfollow = (List<String>) documentSnapshot.get("whoIfollow");
                    query = FirebaseUtil.getUsersCollectionReference().whereIn("userId", whoIfollow);
                }

                FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                        .setQuery(query, UserModel.class).build();

                adapter = new SearchUserRecyclerAdapter(options, getApplicationContext());
                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                recyclerView.setAdapter(adapter);
                adapter.startListening();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}