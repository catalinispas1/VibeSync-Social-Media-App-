package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import org.meicode.socialmediaapp.fragments.ChatFragment;
import org.meicode.socialmediaapp.fragments.HomeFragment;
import org.meicode.socialmediaapp.fragments.NewPostFragment;
import org.meicode.socialmediaapp.fragments.ProfileFragment;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.utils.FirebaseUtil;
import org.meicode.socialmediaapp.utils.PrepareUserFeed;

import java.util.List;

public class SplashActivity extends AppCompatActivity {

    public static boolean firstTimeLaunch;
    public static int selectedNavElement;

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
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("fromNotification", 1);
                intent.putExtra("userId", userId);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
            } else {
                PrepareUserFeed prepareUserFeed = new PrepareUserFeed(SplashActivity.this);
                prepareUserFeed.prepareUserFeed();
            }
        }
    }
}