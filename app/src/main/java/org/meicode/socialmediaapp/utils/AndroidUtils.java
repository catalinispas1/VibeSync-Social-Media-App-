package org.meicode.socialmediaapp.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.Timestamp;

import org.meicode.socialmediaapp.model.UserModel;

import java.util.ArrayList;
import java.util.Date;

public class AndroidUtils {
    public static void showToast(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    public static boolean userDataCorrectFormat(Context context,String email, String password) {
        if (email.length() < 5 && password.length() < 8) {
            AndroidUtils.showToast(context, "The data is too short");
            return false;
        }
        if (email.length() < 5) {
            AndroidUtils.showToast(context, "The username is too short");
            return false;
        }
        if (password.length() < 8) {
            AndroidUtils.showToast(context, "The password is too short");
            return false;
        }
        return true;
    }

    public static void setProfileImage(Context context, Uri imageUri, ImageButton imageButton) {
        Glide.with(context).load(imageUri).into(imageButton);
    }

    public static void setProfileImage(Context context, Uri imageUri, ImageView imageview) {
        Glide.with(context).load(imageUri).apply(RequestOptions.circleCropTransform()).into(imageview);
    }

    public static void setPostImage(Context context, Uri imageUri, ImageView imageview) {
        Glide.with(context).load(imageUri).into(imageview);
    }



    public static CharSequence postedTime(Timestamp postTimestamp) {
        Date date = postTimestamp.toDate();
        long  timestamp = date.getTime();

        return DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
    }
}
