package com.example.nextalkapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.nextalkapp.Model.OfflineMessage;

import java.util.ArrayList;
import java.util.List;

public class OfflineDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "NexTalkOffline.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "pending_messages";

    public OfflineDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "messageId TEXT, " +
                "sender TEXT, " +
                "receiver TEXT, " +
                "message TEXT, " +
                "type TEXT, " +
                "timestamp LONG, " +
                "chatRoomId TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addMessage(OfflineMessage msg) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("messageId", msg.getMessageId());
        values.put("sender", msg.getSender());
        values.put("receiver", msg.getReceiver());
        values.put("message", msg.getMessage());
        values.put("type", msg.getType());
        values.put("timestamp", msg.getTimestamp());
        values.put("chatRoomId", msg.getChatRoomId());
        db.insert(TABLE_NAME, null, values);
    }

    public List<OfflineMessage> getAllPendingMessages() {
        List<OfflineMessage> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        if (cursor.moveToFirst()) {
            do {
                OfflineMessage msg = new OfflineMessage();
                msg.setMessageId(cursor.getString(1));
                msg.setSender(cursor.getString(2));
                msg.setReceiver(cursor.getString(3));
                msg.setMessage(cursor.getString(4));
                msg.setType(cursor.getString(5));
                msg.setTimestamp(cursor.getLong(6));
                msg.setChatRoomId(cursor.getString(7));
                list.add(msg);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void deleteMessage(String messageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "messageId = ?", new String[]{messageId});
    }

    public void clearTable() {
        getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME);
    }
}
