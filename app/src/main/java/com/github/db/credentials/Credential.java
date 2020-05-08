package com.github.db.credentials;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity
public class Credential implements Serializable {
    @NonNull
    @PrimaryKey
    public short id = 1;

    @ColumnInfo(name = "username")
    public String username;

    @ColumnInfo(name = "password")
    public String password;

    @ColumnInfo(name = "ipAddress")
    public String ipAddress;

    public Credential(String username, String password, String ipAddress) {
        this.id = 1;
        this.username = username;
        this.password = password;
        this.ipAddress = ipAddress;
    }
}
