package org.meicode.socialmediaapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.UploadTask;

import org.meicode.socialmediaapp.R;
import org.meicode.socialmediaapp.model.PostModel;
import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class NewPostFragment extends Fragment {

    ActivityResultLauncher<Intent> imagePickerLauncher;
    Uri selectedImageUri;
    ImageButton uploadPostImage;
    EditText postDescriptionInput;
    Button uploadPostButton;
    PostModel postModel;
    ProgressBar addPostProgressBar;

    public NewPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            if (getContext() != null)
                                AndroidUtils.setProfileImage(getContext(), selectedImageUri, uploadPostImage);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        postModel = new PostModel();
        View view = inflater.inflate(R.layout.fragment_new_post, container, false);
        uploadPostImage = view.findViewById(R.id.post_imageview);
        postDescriptionInput = view.findViewById(R.id.post_description);
        uploadPostButton = view.findViewById(R.id.upload_post_btn);
        addPostProgressBar = view.findViewById(R.id.add_post_progressbar);

        uploadPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePicker.with(NewPostFragment.this).cropSquare()
                        .createIntent(new Function1<Intent, Unit>() {
                            @Override
                            public Unit invoke(Intent intent) {
                                imagePickerLauncher.launch(intent);
                                return null;
                            }
                        });
            }
        });

        uploadPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedImageUri == null) {
                    AndroidUtils.showToast(getContext().getApplicationContext(), "Select an image");
                    return;
                }
                String postDescription = postDescriptionInput.getText().toString();
                uploadPostButton.setVisibility(View.INVISIBLE);
                addPostProgressBar.setVisibility(View.VISIBLE);

                postModel.setPostDescription(postDescription);
                postModel.setUploadTime(Timestamp.now());
                postModel.setUserId(FirebaseUtil.getCurrentUserId());
                FirebaseUtil.getCurrentUserDetails().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            postModel.setUsername(task.getResult().toObject(UserModel.class).getUsername());
                            uploadPost();
                        } else {
                            uploadPostButton.setVisibility(View.VISIBLE);
                            addPostProgressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });

            }
        });

        return view;
    }

    private void uploadPost() {
        FirebaseUtil.getUserPosts(FirebaseUtil.getCurrentUserId()).add(postModel).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    String postId = task.getResult().getId();

                    FirebaseUtil.getUsersCollectionReference().document(FirebaseUtil.getCurrentUserId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            List<String> whoFollowsMe = (List<String>) documentSnapshot.get("whoFollowsMe");
                            FirebaseUtil.getUserPosts(FirebaseUtil.getCurrentUserId()).document(postId).update("userThatWillSeePostOnFeed", whoFollowsMe);
                        }
                    });
                    uploadImageToStorage(postId);
                } else {
                    uploadPostButton.setVisibility(View.VISIBLE);
                    addPostProgressBar.setVisibility(View.INVISIBLE);
                    AndroidUtils.showToast(getContext(), "Post upload failed");
                }
            }
        });
    }

    private void uploadImageToStorage(String postId) {
        FirebaseUtil.getPostImageStorageReference(postId, FirebaseUtil.getCurrentUserId()).putFile(selectedImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    AndroidUtils.showToast(getContext(), "Post uploaded successfully");
                    postDescriptionInput.setText("");
                    uploadPostImage.setImageResource(R.drawable.camera_icon);
                    selectedImageUri = null;
                } else {
                    AndroidUtils.showToast(getContext(), "Post upload failed");
                }
                uploadPostButton.setVisibility(View.VISIBLE);
                addPostProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }
}