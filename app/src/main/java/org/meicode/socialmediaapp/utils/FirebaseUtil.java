package org.meicode.socialmediaapp.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class FirebaseUtil {

    public static FirebaseAuth getFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    public static String getCurrentUserId() {
        return getFirebaseAuth().getUid();
    }

    public static boolean isLoggedIn() {
        return getCurrentUserId() != null;
    }

    public static CollectionReference getUsersCollectionReference() {
        return FirebaseFirestore.getInstance().collection("users");
    }

    public static DocumentReference getCurrentUserDetails() {
        return getUsersCollectionReference().document(getCurrentUserId());
    }

    public static CollectionReference getUserPosts (String userId) {
        return getUsersCollectionReference().document(userId).collection("posts");
    }

    public static StorageReference getPostImageStorageReference (String postId, String userId) {
        return FirebaseStorage.getInstance().getReference().child("posts_image").child(userId).child(postId);
    }

    public static StorageReference getProfilePicStorageReference (String userId) {
        return FirebaseStorage.getInstance().getReference().child("profile_pics").child(userId);
    }

    public static CollectionReference getPostComments(String userId, String postId) {
        return getUsersCollectionReference().document(userId).collection("posts").document(postId).collection("comments");
    }

    public static DocumentReference setUserFeed(String userId, String postId) {
        return FirebaseFirestore.getInstance().collection("feed_" + userId).document(postId);
    }

    public static CollectionReference getUserFeed(String userIdFeed) {
        return FirebaseFirestore.getInstance().collection("feed_" + userIdFeed);
    }

    public static DocumentReference getChatRoomReference(String chatRoomId) {
        return FirebaseFirestore.getInstance().collection("chatrooms").document(chatRoomId);
    }

    public static CollectionReference getChatRoomMessageReference(String chatRoomId) {
        return getChatRoomReference(chatRoomId).collection("chats");
    }

    public static String getChatRoomId(String userId1, String userId2) {
        if (userId1.hashCode() < userId2.hashCode()) return userId1 + "_" + userId2;
        else return userId2 + "_" + userId1;
    }

    public static CollectionReference allChatroomCollectionReference() {
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds) {
        if (userIds.get(0).equals(FirebaseUtil.getCurrentUserId())) {
            return getUsersCollectionReference().document(userIds.get(1));
        } else {
            return getUsersCollectionReference().document(userIds.get(0));
        }
    }
}
