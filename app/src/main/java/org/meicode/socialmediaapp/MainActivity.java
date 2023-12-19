package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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
import org.meicode.socialmediaapp.utils.PrepareUserFeed;


public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    ImageButton searchButton;
    ImageButton refreshButton;
    ImageButton logoutButton;
    HomeFragment homeFragment;
    ChatFragment chatFragment;
    NewPostFragment newPostFragment;
    ProfileFragment profileFragment;
    FrameLayout homeFrameLayout;
    FrameLayout frameLayout;
    TextView fragmentTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeFragment = new HomeFragment();
        chatFragment = new ChatFragment();
        newPostFragment = new NewPostFragment();
        profileFragment = new ProfileFragment();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        searchButton = findViewById(R.id.search_btn);
        refreshButton = findViewById(R.id.refresh_btn);
        logoutButton = findViewById(R.id.logout_btn);

        homeFrameLayout = findViewById(R.id.home_frame_layout);
        frameLayout = findViewById(R.id.main_frame_layout);
        fragmentTitle = findViewById(R.id.fragment_title);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SearchUserActivity.class));
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
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

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PrepareUserFeed prepareUserFeed = new PrepareUserFeed();
                prepareUserFeed.prepareUserFeed();
            }
        });

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int frameId = R.id.main_frame_layout;
                int homeFrameId = R.id.home_frame_layout;

                if (item.getItemId() == R.id.home) {
                    replaceFragment(frameId, homeFrameId, homeFragment);
                    fragmentTitle.setText("Feed");
                    logoutButton.setVisibility(View.GONE);
                    searchButton.setVisibility(View.VISIBLE);
                } else if (item.getItemId() == R.id.chat) {
                    replaceFragment(frameId, homeFrameId, chatFragment);
                    fragmentTitle.setText("Recent chats");
                    logoutButton.setVisibility(View.GONE);
                    searchButton.setVisibility(View.VISIBLE);


                } else if (item.getItemId() == R.id.new_post) {
                    replaceFragment(frameId, homeFrameId, newPostFragment);
                    fragmentTitle.setText("Add a new photo");
                    logoutButton.setVisibility(View.GONE);
                    searchButton.setVisibility(View.VISIBLE);
                } else {
                    replaceFragment(frameId, homeFrameId, profileFragment);
                    fragmentTitle.setText("Your Profile");
                    logoutButton.setVisibility(View.VISIBLE);
                    searchButton.setVisibility(View.GONE);
                }
                return true;
            }
        });
        bottomNavigationView.setSelectedItemId(R.id.home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getFcmToken();
    }

    private Fragment previousFragment;
    private void replaceFragment(int mainFrame, int homeFrameId, Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (fragment == homeFragment) {
            refreshButton.setVisibility(View.VISIBLE);
            if (SplashActivity.firstTimeLaunch) {
                transaction.replace(homeFrameId, fragment).commit();
//                SplashActivity.firstTimeLaunch = false; this is setted up to false in the homefragment
            } else {
                if (previousFragment != null) {
                    transaction.remove(previousFragment).commit();
                }
            }
            frameLayout.setVisibility(View.INVISIBLE);
            homeFrameLayout.setVisibility(View.VISIBLE);
        }
        else {
            refreshButton.setVisibility(View.INVISIBLE);
            frameLayout.setVisibility(View.VISIBLE);
            homeFrameLayout.setVisibility(View.INVISIBLE);
            previousFragment = fragment;

            transaction.replace(mainFrame, fragment).commit();
        }
    }
    private void getFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    FirebaseUtil.getCurrentUserDetails().update("fcmToken", token);
                }
            }
        });
    }
}