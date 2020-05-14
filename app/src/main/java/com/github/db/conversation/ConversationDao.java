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

    @Query("SELECT * FROM ConversationMessage WHERE conversation LIKE :conversation ORDER BY timestamp DESC LIMIT 1")
    ConversationMessage getLastMessageFromConversation(String conversation);

    @Query("DELETE FROM ConversationMessage WHERE timestamp LIKE :timestamp AND messageType LIKE :messageType")
    void removeConversationMessage(long timestamp, short messageType);

    @Query("DELETE FROM ConversationMessage WHERE conversation LIKE :conversation")
    void removeConversations(String conversation);

    @Query("UPDATE ConversationMessage SET success = 1 WHERE timestamp = :id")
    void setAsSuccess(long id);

    @Insert
    void insert(ConversationMessage conversationMessage);
}
