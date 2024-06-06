package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

import org.meicode.socialmediaapp.fragments.ChatFragment;
import org.meicode.socialmediaapp.fragments.HomeFragment;
import org.meicode.socialmediaapp.fragments.NewPostFragment;
import org.meicode.socialmediaapp.fragments.ProfileFragment;
import org.meicode.socialmediaapp.utils.FirebaseUtil;


public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    ImageButton searchButton;
    ImageButton refreshButton;
    ImageButton logoutButton;
    FrameLayout frameLayout;
    TextView fragmentTitle;
    HomeFragment homeFragment;
    ChatFragment chatFragment;
    NewPostFragment newPostFragment;
    ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        refreshButton = findViewById(R.id.refresh_btn);

        homeFragment = new HomeFragment();
        homeFragment.setRefreshButton(refreshButton);
        chatFragment = new ChatFragment();
        newPostFragment = new NewPostFragment();
        profileFragment = new ProfileFragment();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        searchButton = findViewById(R.id.search_btn);
        logoutButton = findViewById(R.id.logout_btn);

        frameLayout = findViewById(R.id.main_frame_layout);
        frameLayout.removeAllViews();
        fragmentTitle = findViewById(R.id.fragment_title);

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    FirebaseUtil.getCurrentUserDetails().update("fcmToken", task.getResult());
                }
            }
        });
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SearchUserActivity.class));
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
             public void onClick(View view) {
                showAssureDialog();
            }
        });

        int frameId = R.id.main_frame_layout;

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                SplashActivity.selectedNavElement = item.getItemId();
                if (item.getItemId() == R.id.home) {
                    replaceFragment(frameId, homeFragment);
                    fragmentTitle.setText("Feed");
                    logoutButton.setVisibility(View.GONE);
                    searchButton.setVisibility(View.VISIBLE);
                    refreshButton.setVisibility(View.VISIBLE);
                } else if (item.getItemId() == R.id.chat) {
                    replaceFragment(frameId, chatFragment);
                    fragmentTitle.setText("Recent chats");
                    logoutButton.setVisibility(View.GONE);
                    searchButton.setVisibility(View.VISIBLE);
                    refreshButton.setVisibility(View.INVISIBLE);
                } else if (item.getItemId() == R.id.new_post) {
                    replaceFragment(frameId, newPostFragment);
                    fragmentTitle.setText("Add a new photo");
                    logoutButton.setVisibility(View.GONE);
                    searchButton.setVisibility(View.VISIBLE);
                    refreshButton.setVisibility(View.INVISIBLE);
                } else {
                    replaceFragment(frameId, profileFragment);
                    fragmentTitle.setText("Your Profile");
                    logoutButton.setVisibility(View.VISIBLE);
                    searchButton.setVisibility(View.GONE);
                    refreshButton.setVisibility(View.INVISIBLE);
                }
                return true;
            }
        });

        if (savedInstanceState == null) {
            if (getIntent().getExtras() != null && getIntent().getExtras().getInt("fromNotification") == 1) {
                bottomNavigationView.setSelectedItemId(R.id.chat);
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("fromNotification", 1);
                intent.putExtra("userId", getIntent().getExtras().getString("userId"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            } else {
                bottomNavigationView.setSelectedItemId(R.id.home);
            }
        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            for (Fragment fragment : fragmentManager.getFragments()) {
                transaction.remove(fragment);
            }
            transaction.commit();
            bottomNavigationView.setSelectedItemId(SplashActivity.selectedNavElement);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v("TAG", "ON CONFIG CALLED");
    }

    Fragment previousFragment;
    private void replaceFragment(int mainFrame, Fragment targetFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (previousFragment != null) {
            transaction.hide(previousFragment);
        }
        if (targetFragment.isAdded()) {
            transaction.show(targetFragment);
            Log.v("TAG", "FRAGMENT SHOWED");
        } else {
            transaction.add(mainFrame, targetFragment);
            Log.v("TAG", "FRAGMENT ADDED FOR THE FIRST TIME");
        }

        previousFragment = targetFragment;
        transaction.commit();
    }

    private void showAssureDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.assure_dialog, null);
        Button cancelButton = view.findViewById(R.id.cancel_button);
        Button deleteButton = view.findViewById(R.id.delete_button);
        deleteButton.setText("Log Out");
        TextView assureTextview = view.findViewById(R.id.textviewAssure);
        assureTextview.setText("Are you sure you want to logout?");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        AlertDialog alertDialog = builder.create();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            SplashActivity.firstTimeLaunch = true;
                            startActivity(intent);
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }
}