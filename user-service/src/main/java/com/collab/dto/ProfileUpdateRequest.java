package com.collab.dto;

public class ProfileUpdateRequest {
    private String fullName;
    private String bio;
    private String profilePictureUrl;

    public ProfileUpdateRequest() {}

    public ProfileUpdateRequest(String fullName, String bio, String profilePictureUrl) {
        this.fullName = fullName;
        this.bio = bio;
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
