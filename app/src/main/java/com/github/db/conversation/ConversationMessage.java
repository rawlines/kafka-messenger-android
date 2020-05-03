package com.github.db.conversation;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.github.utils.Cryptography;

import java.io.Serializable;

@Entity(primaryKeys = {"timestamp", "messageType"})
public class ConversationMessage implements Serializable {
    public static final short SENT = 0;
    public static final short RECEIBED = 1;

    public static class MetaData {
        public long timestamp;
        public String plain;
        public String source;
    }

    @NonNull
    @ColumnInfo(name = "timestamp")
    public long timestamp = 0;

    @ColumnInfo(name = "conversation")
    public String conversation;

    @ColumnInfo(name = "content")
    public byte[] content;

    @ColumnInfo(name = "messageType")
    public short messageType;

    @ColumnInfo(name = "success")
    public boolean success = false;

    public ConversationMessage() { }

    @Ignore
    public ConversationMessage(byte[] content) {
        this.content = content;
    }

    public static ConversationMessage fromCryptedBytes(byte[] bytes) {
        ConversationMessage msg = new ConversationMessage(bytes);


        MetaData md = Cryptography.parseCrypted(bytes);
        msg.timestamp = md.timestamp;
        msg.conversation = md.source;


        return msg;
    }
}
