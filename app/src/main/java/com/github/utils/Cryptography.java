package com.github.utils;

import android.util.Log;

import com.github.db.conversation.ConversationMessage.MetaData;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

public class Cryptography {
      public static MetaData parseCrypted(byte[] bytes) {
            String s = new String(bytes, StandardCharsets.ISO_8859_1);
            Log.d("CONS", "Parsing; " + s);
            Gson gson = new Gson();
            MetaData md = gson.fromJson(s, MetaData.class);

            return md;
      }

      public static byte[] metadataToCryptedBytes(MetaData md) {
            Gson gson = new Gson();
            String json = gson.toJson(md, MetaData.class);
            Log.d("CONS", "Transforming; " + json);
            return json.getBytes(StandardCharsets.ISO_8859_1);
      }
}
