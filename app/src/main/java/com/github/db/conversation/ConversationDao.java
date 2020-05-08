package com.github.db.conversation;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.github.db.contact.Contact;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface ConversationDao {
    @Query("SELECT * FROM ConversationMessage WHERE conversation LIKE :conversation ORDER BY timestamp ASC")
    List<ConversationMessage> getMessagesFromConversation(String conversation);

    @Query("SELECT * FROM ConversationMessage WHERE success = 0")
    List<ConversationMessage> getUnsuccess();

    @Query("SELECT * FROM ConversationMessage WHERE timestamp LIKE :timestamp AND messageType LIKE :type")
    ConversationMessage getUniqueMessage(long timestamp, short type);

    @Query("UPDATE ConversationMessage SET success = 1 WHERE timestamp = :id")
    void setAsSuccess(long id);

    @Insert
    void insert(ConversationMessage conversationMessage);
}
