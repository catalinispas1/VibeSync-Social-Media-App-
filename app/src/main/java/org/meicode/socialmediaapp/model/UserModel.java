package org.meicode.socialmediaapp.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class UserModel {
    private String username;
    private Timestamp createdTimestamp;
    private String bio;
    private List<String> whoIfollow;
    private List<String> whoFollowsMe;
    private String userId;
    String fcmToken;

    public UserModel() {
        whoFollowsMe = new ArrayList<>();
        whoIfollow = new ArrayList<>();
    }

    public UserModel(String username, Timestamp createdTimestamp, String bio, List<String> whoIfollow, List<String> whoFollowsMe, String userId) {
        this.username = username;
        this.createdTimestamp = createdTimestamp;
        this.bio = bio;
        this.whoIfollow = whoIfollow;
        this.whoFollowsMe = whoFollowsMe;
        this.userId = userId;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Timestamp getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public List<String> getWhoIfollow() {
        return whoIfollow;
    }

    public void setWhoIfollow(List<String> whoIfollow) {
        this.whoIfollow = whoIfollow;
    }

    public List<String> getWhoFollowsMe() {
        return whoFollowsMe;
    }

    public void setWhoFollowsMe(List<String> whoFollowsMe) {
        this.whoFollowsMe = whoFollowsMe;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
