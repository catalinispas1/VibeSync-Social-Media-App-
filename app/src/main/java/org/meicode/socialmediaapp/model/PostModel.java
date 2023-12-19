package org.meicode.socialmediaapp.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class PostModel {
    private String postDescription;
    private Timestamp uploadTime;
    private List<String> userThatLiked;
    private List<String> userThatWillSeePostOnFeed;
    private int commentsCount;
    private String userId;
    private String username;

    public PostModel() {
        userThatLiked = new ArrayList<>();
    }

    public PostModel(String postDescription, Timestamp uploadTime, List<String> userThatLiked, List<String> userThatWillSeePostOnFeed, int commentsCount, String userId, String username) {
        this.postDescription = postDescription;
        this.uploadTime = uploadTime;
        this.userThatLiked = userThatLiked;
        this.userThatWillSeePostOnFeed = userThatWillSeePostOnFeed;
        this.commentsCount = commentsCount;
        this.userId = userId;
        this.username = username;
    }

    public String getPostDescription() {
        return postDescription;
    }

    public void setPostDescription(String postDescription) {
        this.postDescription = postDescription;
    }

    public Timestamp getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Timestamp uploadTime) {
        this.uploadTime = uploadTime;
    }

    public List<String> getUserThatLiked() {
        return userThatLiked;
    }

    public void setUserThatLiked(List<String> userThatLiked) {
        this.userThatLiked = userThatLiked;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getUserThatWillSeePostOnFeed() {
        return userThatWillSeePostOnFeed;
    }

    public void setUserThatWillSeePostOnFeed(List<String> userThatWillSeePostOnFeed) {
        this.userThatWillSeePostOnFeed = userThatWillSeePostOnFeed;
    }
}
