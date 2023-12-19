package org.meicode.socialmediaapp.adapters;

import android.view.View;

public interface PostClickListener {
    void onPostClicked(View view, int position, String userId);
}
