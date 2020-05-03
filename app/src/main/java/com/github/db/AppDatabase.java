package com.github.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.github.db.contact.Contact;
import com.github.db.contact.ContactDao;
import com.github.db.conversation.ConversationDao;
import com.github.db.conversation.ConversationMessage;

@Database(entities = {Contact.class, ConversationMessage.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ContactDao contactDao();
    public abstract ConversationDao conversationDao();
}
