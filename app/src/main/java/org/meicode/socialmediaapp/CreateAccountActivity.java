package org.meicode.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.QuerySnapshot;

import org.meicode.socialmediaapp.model.UserModel;
import org.meicode.socialmediaapp.utils.AndroidUtils;
import org.meicode.socialmediaapp.utils.FirebaseUtil;

public class CreateAccountActivity extends AppCompatActivity {

    EditText emailInput, passwordInput, usernameInput;
    Button createAccountBtn;
    ImageButton goBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        usernameInput = findViewById(R.id.username_input);
        createAccountBtn = findViewById(R.id.create_account_btn);
        goBackButton = findViewById(R.id.go_back_button);

        emailInput.setText("");
        passwordInput.setText("");
        usernameInput.setText("");

        createAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailInput.getText().toString();
                String username = usernameInput.getText().toString();
                String password = passwordInput.getText().toString();
                if (!AndroidUtils.userDataCorrectFormat(CreateAccountActivity.this, username, password)) {
                    return;
                }
                createUniqueUsernameAccount(username, email, password);
            }
        });

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void createUniqueUsernameAccount(String username, String email, String password) {
        FirebaseUtil.getUsersCollectionReference().whereEqualTo("username", username).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (!task.getResult().isEmpty()) {
                        usernameInput.setError("Username already in use");
                    } else createUserAccount(email, password, username);
                } else {
                    AndroidUtils.showToast(CreateAccountActivity.this, "Error checking username");
                }
            }
        });
    }

    private void createUserAccount(String email, String password, String username) {
        FirebaseUtil.getFirebaseAuth().createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    UserModel userModel = new UserModel();
                    userModel.setUsername(username);
                    userModel.setCreatedTimestamp(Timestamp.now());
                    userModel.setUserId(FirebaseUtil.getCurrentUserId());

                    FirebaseUtil.getCurrentUserDetails().set(userModel);
                    Intent intent = new Intent(CreateAccountActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                    finish();
                } else {
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        emailInput.setError("Email already in use");
                    } else {
                        AndroidUtils.showToast(CreateAccountActivity.this, "Failed to create user account");
                    }
                }
            }
        });
    }
}