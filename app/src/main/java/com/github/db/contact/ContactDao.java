package com.github.db.contact;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ContactDao {
    @Query("SELECT * FROM Contact WHERE username LIKE :username")
    Contact getContact(String username);

    @Query("UPDATE Contact SET publicKey = :publicKey WHERE username LIKE :username")
    void updatePublicKey(String username, byte[] publicKey);

    @Query("UPDATE Contact SET alias = :alias WHERE username LIKE :username")
    void updateAlias(String username, String alias);

    @Query("SELECT * FROM Contact WHERE (SELECT count(*) FROM ConversationMessage WHERE conversation LIKE username) > 0")
    List<Contact> getActive();

    @Query("SELECT * FROM Contact WHERE (SELECT count(*) FROM ConversationMessage WHERE conversation LIKE username) = 0")
    List<Contact> getInactive();

    @Query("UPDATE Contact SET unread = :value WHERE username LIKE :username")
    void setUnread(String username, boolean value);

    @Insert
    void insert(Contact contact);
}
