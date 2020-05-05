package com.github.db.credentials;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Credential {
    @NonNull
    @PrimaryKey
    public short id = 1;

    @ColumnInfo(name = "username")
    public String username;

    @ColumnInfo(name = "password")
    public String password;

    public Credential(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
