package com.example.nextalkapp.Model;

public class User {
    public String uid;
    public String name;
    public String phone;
    public String bio;
    public String avatar;
    public String lastMessage;
    public long lastTime;

    public User() {
        // Firebase cần constructor rỗng
    }

    public User(String uid, String name, String phone, String bio, String avatar, String lastMessage, long lastTime) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.bio = bio;
        this.avatar = avatar;
        this.lastMessage = lastMessage;
        this.lastTime = lastTime;
    }

    public User(String uid, String name, String avatar, String lastMessage, long lastTime) {
        this.uid = uid;
        this.name = name;
        this.avatar = avatar;
        this.lastMessage = lastMessage;
        this.lastTime = lastTime;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }
}