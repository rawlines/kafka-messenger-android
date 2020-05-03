package com.github.db;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.db.contact.Contact;
import com.github.db.contact.ContactDao;
import com.github.db.conversation.ConversationDao;
import com.github.db.conversation.ConversationMessage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class DatabaseManager {
      public interface Callback<T> { void call(T o); }

      private static DatabaseManager instance = null;

      private Handler mainLoop = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                  Bundle b = msg.getData();

                  Object o = b.getSerializable("arg");
                  ArrayList<DatabaseManager.Callback> q = (ArrayList<DatabaseManager.Callback>) b.getSerializable("queue");

                  for (DatabaseManager.Callback c : q)
                        c.call(o);
            }
      };

      private AppDatabase db;
      private ConversationDao conversationDao;
      private ContactDao contactDao;

      private HashMap<String, ArrayList<Callback<ConversationMessage>>> conversationCallbacksMap = new HashMap<>();
      private HashMap<String, ArrayList<Callback<ConversationMessage>>> successCallbacksMap = new HashMap<>();
      private ArrayList<Callback<ConversationMessage>> allConversationCallbacks = new ArrayList<>();

      private DatabaseManager(AppDatabase db) {
            this.db = db;
            this.conversationDao = db.conversationDao();
            this.contactDao = db.contactDao();
      }

      public static DatabaseManager init(@NonNull AppDatabase db) {
            if (instance == null) {
                  instance = new DatabaseManager(db);
                  return instance;
            }

            return null;
      }

      private <T extends Serializable> void doCallback(@NonNull T o, @NonNull ArrayList<Callback<T>> callbacks) {
            Bundle b = new Bundle();
            Message msg = new Message();

            b.putSerializable("arg", o);
            b.putSerializable("queue", callbacks);
            msg.setData(b);

            mainLoop.sendMessage(msg);
      }

      public synchronized boolean messageExists(@NonNull ConversationMessage msg) {
            return conversationDao.getUniqueMessage(msg.timestamp, msg.messageType) != null;
      }

      public synchronized void insertConversationMessage(@NonNull ConversationMessage msg) {
            String conv = msg.conversation;
            if (messageExists(msg))
                  return;

            conversationDao.insert(msg);

            ArrayList<Callback<ConversationMessage>> cs = conversationCallbacksMap.get(conv);

            if (cs != null)
                  cs.addAll(allConversationCallbacks);
            else
                  cs = allConversationCallbacks;

            setContactLastMessage(msg.conversation, msg.timestamp, msg.messageType);

            doCallback(msg, cs);
      }

      public synchronized void addConversationCallback(@Nullable String conversation, @NonNull Callback<ConversationMessage> callback) {
            if (conversation == null) {
                  allConversationCallbacks.add(callback);
            } else {
                  ArrayList<Callback<ConversationMessage>> callbacks =
                        conversationCallbacksMap.getOrDefault(conversation, new ArrayList<>());

                  callbacks = callbacks == null ? new ArrayList<>() : callbacks;
                  callbacks.add(callback);
                  this.conversationCallbacksMap.put(conversation, callbacks);
            }
      }

      public synchronized void addSuccessCallback(@NonNull String conversation, @NonNull Callback<ConversationMessage> callback) {
            ArrayList<Callback<ConversationMessage>> callbacks =
                  successCallbacksMap.getOrDefault(conversation, new ArrayList<>());

            callbacks = callbacks == null ? new ArrayList<>() : callbacks;
            callbacks.add(callback);
            this.successCallbacksMap.put(conversation, callbacks);
      }

      public synchronized ConversationMessage getConversationMessages(long timestamp, short type) {
            return conversationDao.getUniqueMessage(timestamp, type);
      }

      public synchronized List<ConversationMessage> getConversationMessages(String c) {
            return conversationDao.getMessagesFromConversation(c);
      }

      public synchronized List<ConversationMessage> getUnsuccessMessages() {
            return conversationDao.getUnsuccess();
      }

      public synchronized void setMessageAsSuccess(ConversationMessage msg) {
            this.conversationDao.setAsSuccess(msg.timestamp);

            ArrayList<Callback<ConversationMessage>> cs = successCallbacksMap.get(msg.conversation);

            if (cs != null)
                  doCallback(msg, cs);
      }

      public synchronized void insertContact(@NonNull Contact contact) {
            this.contactDao.insert(contact);
      }

      public synchronized void setContactLastMessage(String username, long time, short type) {
            this.contactDao.setLastMessage(username, time, type);
      }

      public synchronized List<Contact> getContacts() {
            return this.contactDao.getInactive();
      }

      public synchronized List<Contact> getChats() {
            return this.contactDao.getActive();
      }
}
