package com.github.db;

import android.content.Context;

import androidx.room.Room;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class DatabaseUtil {
    public static AppDatabase getDatabase(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class,
                "application-db").build();
    }

    public static String listToJson(List l) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("messages", l);
        return json.toString();
    }
}
