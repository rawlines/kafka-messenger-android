package com.github.db.contact;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ContactDao {
    @Query("SELECT * FROM Contact WHERE username LIKE :username")
    Contact getContact(String username);

    @Query("SELECT * FROM Contact WHERE lastMessageTime != 0 AND lastMessageType != -1")
    List<Contact> getActive();

    @Query("SELECT * FROM Contact WHERE lastMessageTime = 0 AND lastMessageType = -1")
    List<Contact> getInactive();

    @Query("UPDATE Contact SET lastMessageTime = :time, lastMessageType = :type WHERE username LIKE :username")
    void setLastMessage(String username, long time, short type);

    @Insert
    void insert(Contact contact);
}
