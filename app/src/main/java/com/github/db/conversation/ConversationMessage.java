package com.github.db.conversation;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;

import com.github.db.contact.Contact;
import com.github.crypto.Cryptography;

import java.io.Serializable;
import java.security.PrivateKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(primaryKeys = {"timestamp", "messageType"},
        foreignKeys = @ForeignKey(entity = Contact.class,
                        parentColumns = "username",
                        childColumns = "conversation",
                        onDelete = CASCADE))
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
    public String conversation; //foreign key, references username from Contact

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

    public static ConversationMessage fromCryptedBytes(byte[] bytes) throws Exception {
        ConversationMessage msg = new ConversationMessage(bytes);

        MetaData md = Cryptography.decryptBytes(bytes);
        msg.timestamp = md.timestamp;
        msg.conversation = md.source;

        return msg;
    }
}
