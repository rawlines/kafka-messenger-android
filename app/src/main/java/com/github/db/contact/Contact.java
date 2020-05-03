package com.github.db.contact;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity
public class Contact implements Serializable {
    @NonNull
    @PrimaryKey
    public String username;

    @ColumnInfo(name = "alias")
    public String alias;

    @ColumnInfo(name = "publicKey")
    public byte[] publicKey;

    @ColumnInfo(name = "lastMessageTime")
    public long lastMessageTime = 0;

    @ColumnInfo(name = "lastMessageType")
    public short lastMessageType = -1;

    public Contact(String username, String alias, byte[] publicKey) {
        this.username = username;
        this.alias = alias;
        this.publicKey = publicKey;
    }
}
