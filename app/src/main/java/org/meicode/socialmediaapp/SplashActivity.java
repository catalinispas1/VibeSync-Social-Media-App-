package org.meicode.socialmediaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.utils.FirebaseUtil;
import org.meicode.socialmediaapp.utils.PrepareUserFeed;

import java.util.List;

public class SplashActivity extends AppCompatActivity {

    public static boolean firstTimeLaunch;

    @Override
    protected void onResume() {
        super.onResume();
        firstTimeLaunch = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (!FirebaseUtil.isLoggedIn()) {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        } else {

            if (getIntent() != null && getIntent().hasExtra("userId")) {
                String userId = getIntent().getExtras().getString("userId");
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("getFeed", 1);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
            } else {
                Log.v("TAG", "INTENT IS NULL");
                PrepareUserFeed prepareUserFeed = new PrepareUserFeed(SplashActivity.this);
                prepareUserFeed.prepareUserFeed();
            }
        }
    }
}