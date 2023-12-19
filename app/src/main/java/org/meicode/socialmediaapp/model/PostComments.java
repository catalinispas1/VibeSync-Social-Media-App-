package org.meicode.socialmediaapp.model;

import com.google.firebase.Timestamp;

public class PostComments {
    private String comment;
    private Timestamp commentTimestamp;
    private String commentatorId;
    private String commentatorUsername;

    public PostComments() {
    }

    public PostComments(String comment, Timestamp commentTimestamp, String commentatorId, String commentatorUsername) {
        this.comment = comment;
        this.commentTimestamp = commentTimestamp;
        this.commentatorId = commentatorId;
        this.commentatorUsername = commentatorUsername;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Timestamp getCommentTimestamp() {
        return commentTimestamp;
    }

    public void setCommentTimestamp(Timestamp commentTimestamp) {
        this.commentTimestamp = commentTimestamp;
    }

    public String getCommentatorId() {
        return commentatorId;
    }

    public void setCommentatorId(String commentatorId) {
        this.commentatorId = commentatorId;
    }

    public String getCommentatorUsername() {
        return commentatorUsername;
    }

    public void setCommentatorUsername(String commentatorUsername) {
        this.commentatorUsername = commentatorUsername;
    }
}
