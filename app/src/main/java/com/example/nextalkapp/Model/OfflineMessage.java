package com.example.nextalkapp.Model;

public class OfflineMessage {
    private String messageId;
    private String sender;
    private String receiver;
    private String message;
    private String type;
    private long timestamp;
    private String chatRoomId;

    public OfflineMessage() {
    }

    public OfflineMessage(String messageId, String sender, String receiver, String message, String type, long timestamp, String chatRoomId) {
        this.messageId = messageId;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.chatRoomId = chatRoomId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
}
