package org.meicode.socialmediaapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.UploadTask;

import org.checkerframework.checker.units.qual.A;
import org.meicode.socialmediaapp.FollowedUsersActivity;
import org.meicode.socialmediaapp.PostViewActivity;
import org.meicode.socialmediaapp.ProfileViewActivity;
import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.adapters.PostClickListener;
import org.meicode.socialmediaapp.adapters.UserPostsAdapter;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ProfileFragment extends Fragment implements PostClickListener {
    TextView usernameTextview;
    ImageView profilePic;
    ImageButton editProfilePic;
    TextView bioTextview;
    TextView postsCountText;
    TextView whoIfollowTextview;
    TextView whoFollowsMeTextview;
    Button editProfileButton;
    UserModel currentUserModel;
    ActivityResultLauncher<Intent> imagePickerLauncher;
    Uri profilePicUri;
    String username;
    String bioText;
    UserPostsAdapter adapter;
    RecyclerView recyclerView;
    int maxBioLength = 150;
    InputFilter lengthBioFilter = new InputFilter.LengthFilter(maxBioLength);
    int maxUsernameLength = 15;
    InputFilter lengthUsernameFilter = new InputFilter.LengthFilter(maxUsernameLength);

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        profilePicUri = data.getData();
                        AndroidUtils.setProfileImage(getContext(), profilePicUri, profilePic);
                        uploadProfilePic();
                    }
                }
              }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        usernameTextview = view.findViewById(R.id.username_profile);
        profilePic = view.findViewById(R.id.profile_picture);
        bioTextview = view.findViewById(R.id.bio_text);
        editProfilePic = view.findViewById(R.id.edit_profile_pic);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        recyclerView = view.findViewById(R.id.user_posts);
        postsCountText = view.findViewById(R.id.posts_count);
        whoFollowsMeTextview = view.findViewById(R.id.who_follows_me_count);
        whoIfollowTextview = view.findViewById(R.id.who_i_follow);

        whoFollowsMeTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (whoFollowsMeTextview.getText().toString().equals("0\nfollows")) {
                    AndroidUtils.showToast(getContext(), "You have no follows");
                    return;
                }
                Intent intent = new Intent(getContext(), FollowedUsersActivity.class);
                intent.putExtra("userId", FirebaseUtil.getCurrentUserId());
                intent.putExtra("title", "Who follows this user");
                startActivity(intent);
            }
        });

        whoIfollowTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (whoIfollowTextview.getText().toString().equals("0\nfollowing")) {
                    AndroidUtils.showToast(getContext(), "You are following nobody");
                    return;
                }
                Intent intent = new Intent(getContext(), FollowedUsersActivity.class);
                intent.putExtra("userId", FirebaseUtil.getCurrentUserId());
                intent.putExtra("title", "Who is following this user");
                startActivity(intent);
            }
        });

        if (bioText == null) bioText = "";

        editProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditProfileDialog();
            }
        });

        FirebaseUtil.getProfilePicStorageReference(FirebaseUtil.getCurrentUserId()).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    if (isAdded() && getActivity() != null) {
                        AndroidUtils.setProfileImage(getContext(), task.getResult(), profilePic);
                    }
                }
            }
        });

        editProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePicker.with(ProfileFragment.this).cropSquare().compress(512).maxResultSize(512, 512)
                        .createIntent(new Function1<Intent, Unit>() {
                            @Override
                            public Unit invoke(Intent intent) {
                                imagePickerLauncher.launch(intent);
                                return null;
                            }
                        });
            }
        });

        FirebaseUtil.getProfilePicStorageReference(FirebaseUtil.getCurrentUserId()).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    if (getContext() != null) {
                        Glide.with(getContext()).load(task.getResult()).into(profilePic);
                        AndroidUtils.setProfileImage(getContext(), task.getResult(), profilePic);
                    }
                }
            }
        });

        Query query = FirebaseUtil.getUserPosts(FirebaseUtil.getCurrentUserId()).orderBy("uploadTime", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<PostModel> options = new FirestoreRecyclerOptions.Builder<PostModel>()
                .setQuery(query, PostModel.class).build();

        adapter = new UserPostsAdapter(options, getContext(), FirebaseUtil.getCurrentUserId());
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        recyclerView.setAdapter(adapter);
        adapter.setPostClickListener(this);
        adapter.startListening();

        return view;
    }

    private void showEditProfileDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.edit_profile_layout, null);

        EditText newUsername = view.findViewById(R.id.new_username_input);
        EditText newBio = view.findViewById(R.id.new_bio_input);
        Button cancelButton = view.findViewById(R.id.cancel_button);
        Button submitButton = view.findViewById(R.id.submit_button);

        TextView userCharCount = view.findViewById(R.id.user_char_count);
        TextView bioCharCount = view.findViewById(R.id.bio_char_count);

        newUsername.setFilters(new InputFilter[]{lengthUsernameFilter});
        newBio.setFilters(new InputFilter[]{lengthBioFilter});

        newUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                int charCount = newUsername.getText().length();

                userCharCount.setText(maxUsernameLength - charCount + "");

                if (newUsername.getLineCount() > 1) {
                    newUsername.setText(charSequence.subSequence(0, start));
                    newUsername.setSelection(newUsername.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        newBio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                int charCount = newBio.getText().length();

                bioCharCount.setText(maxBioLength - charCount + "");

                if (newBio.getLineCount() > 5) {
                    newBio.setText(charSequence.subSequence(0, start));
                    newBio.setSelection(newBio.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        newUsername.setText(username);
        newBio.setText(bioText);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(view);
        AlertDialog alertDialog = builder.create();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tempNewUserName = newUsername.getText().toString();

                FirebaseUtil.getUsersCollectionReference().whereEqualTo("username", tempNewUserName)
                        .whereNotEqualTo(FieldPath.documentId(), FirebaseUtil.getCurrentUserId()).get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.getResult().isEmpty()) {
                            username = tempNewUserName;
                            bioText = newBio.getText().toString();

                            bioTextview.setText(bioText);
                            usernameTextview.setText(username);

                            currentUserModel.setUsername(username);
                            currentUserModel.setBio(bioText);

                            FirebaseUtil.getCurrentUserDetails().set(currentUserModel);

                            alertDialog.dismiss();
                        } else {
                            newUsername.setError("Username already in use");
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        getUserData();
    }

    private void getUserData() {
        FirebaseUtil.getCurrentUserDetails().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                currentUserModel = task.getResult().toObject(UserModel.class);
                username = currentUserModel.getUsername();
                bioText = currentUserModel.getBio();

                usernameTextview.setText(username);
                bioTextview.setText(bioText);

                whoFollowsMeTextview.setText(currentUserModel.getWhoFollowsMe().size() + "\nfollows");
                whoIfollowTextview.setText(currentUserModel.getWhoIfollow().size() + "\nfollowing");
            }
        });

        FirebaseUtil.getUserPosts(FirebaseUtil.getCurrentUserId()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                postsCountText.setText(task.getResult().size() + "\nposts");
            }
        });
    }

    private void uploadProfilePic () {
        FirebaseUtil.getProfilePicStorageReference(FirebaseUtil.getCurrentUserId()).putFile(profilePicUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    AndroidUtils.showToast(getContext(), "Profile picture changed successfully");
                } else {
                    AndroidUtils.showToast(getContext(), "Something went wrong");
                }
            }
        });
    }


    @Override
    public void onPostClicked(View view, int position, String userId) {
        Intent intent = new Intent(getContext(), PostViewActivity.class);
        intent.putExtra("position", position);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
}