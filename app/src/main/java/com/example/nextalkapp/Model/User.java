package com.example.nextalkapp.Model;

public class User {
    public String uid;
    public String name;
    public String phone;
    public String bio;
    public String avatar;
    public String lastMessage;
    public long lastTime;
    public String status;

    public User() {
        // Firebase cần constructor rỗng
    }

    // 1. Constructor đầy đủ 8 tham số (Dùng cho thông tin cá nhân chi tiết)
    public User(String uid, String name, String phone, String bio, String avatar, String lastMessage, long lastTime, String status) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.bio = bio;
        this.avatar = avatar;
        this.lastMessage = lastMessage;
        this.lastTime = lastTime;
        this.status = status;
    }

    // 2. Constructor 6 tham số (Dùng cho ChatFragment của bạn)
    public User(String uid, String name, String avatar, String lastMessage, long lastTime, String status) {
        this.uid = uid;
        this.name = name;
        this.avatar = avatar;
        this.lastMessage = lastMessage;
        this.lastTime = lastTime;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}