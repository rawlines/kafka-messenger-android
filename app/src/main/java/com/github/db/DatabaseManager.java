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
import com.github.db.credentials.Credential;
import com.github.db.credentials.CredentialsDao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
                  q.forEach((callback) -> callback.call(o));
            }
      };

      private ConversationDao conversationDao;
      private ContactDao contactDao;
      private CredentialsDao credentialsDao;

      /**
       * Structure:
       *    {
       *          conversation: [
       *                id:
       *                callback:
       *          ]
       *    }
       */
      //When a new message is added for an x conversation
      private HashMap<String, ArrayList<HashMap<String,Callback<ConversationMessage>>>> conversationCallbacksMap = new HashMap<>();
      //When a new message is set as correctly sent
      private HashMap<String, ArrayList<HashMap<String, Callback<ConversationMessage>>>> successCallbacksMap = new HashMap<>();
      //when a message is added independenlty of conversation
      private ArrayList<HashMap<String, Callback<ConversationMessage>>> allConversationCallbacks = new ArrayList<>();
      //when credentials are set for first time
      private Callback<Credential> newUserCallback;

      private DatabaseManager(AppDatabase db) {
            this.conversationDao = db.conversationDao();
            this.contactDao = db.contactDao();
            this.credentialsDao = db.credentialsDao();
      }

      public static DatabaseManager init(@NonNull AppDatabase db) {
            if (instance == null)
                  instance = new DatabaseManager(db);

            return instance;
      }

      private <T extends Serializable> void doCallback(@NonNull T o, @NonNull ArrayList<Callback<T>> callbacks) {
            Bundle b = new Bundle();
            Message msg = new Message();

            b.putSerializable("arg", o);
            b.putSerializable("queue", callbacks);
            msg.setData(b);

            mainLoop.sendMessage(msg);
      }

      private synchronized boolean messageExists(@NonNull ConversationMessage msg) {
            return conversationDao.getUniqueMessage(msg.timestamp, msg.messageType) != null;
      }

      public synchronized void insertConversationMessage(@NonNull ConversationMessage msg) {
            String conv = msg.conversation;
            if (messageExists(msg))
                  return;

            conversationDao.insert(msg);


            //do the callbacks
            ArrayList<HashMap<String,Callback<ConversationMessage>>> calls = conversationCallbacksMap.get(conv);

            if (calls != null)
                  calls.addAll(allConversationCallbacks);
            else
                  calls = allConversationCallbacks;

            ArrayList<Callback<ConversationMessage>> finalCallbacks = new ArrayList<>();
            calls.forEach((entry) ->
                  entry.forEach((id, callback) -> finalCallbacks.add(callback))
            );

            doCallback(msg, finalCallbacks);
      }

      public synchronized void addConversationCallback(@NonNull String id, @Nullable String conversation, @NonNull Callback<ConversationMessage> callback) {
            if (conversation == null) {
                  HashMap<String, Callback<ConversationMessage>> hashOfCallbacks = new HashMap<>();
                  hashOfCallbacks.put(id, callback);

                  this.allConversationCallbacks.add(hashOfCallbacks);
            } else {
                  ArrayList<HashMap<String, Callback<ConversationMessage>>> listOfCallbacks = new ArrayList<>();
                  HashMap<String, Callback<ConversationMessage>> hashOfCallbacks = new HashMap<>();

                  hashOfCallbacks.put(id, callback);
                  listOfCallbacks.add(hashOfCallbacks);
                  this.conversationCallbacksMap.put(conversation, listOfCallbacks);
            }
      }

      public synchronized void addMessageSuccessCallback(@NonNull String id, @NonNull String conversation, @NonNull Callback<ConversationMessage> callback) {
            ArrayList<HashMap<String, Callback<ConversationMessage>>> listOfCallbacks = new ArrayList<>();
            HashMap<String, Callback<ConversationMessage>> hashOfCallbacks = new HashMap<>();

            hashOfCallbacks.put(id, callback);
            listOfCallbacks.add(hashOfCallbacks);
            this.successCallbacksMap.put(conversation, listOfCallbacks);
      }

      /**
       * Removes a cellback previously added into Conversation Callbacks
       *
       * @param id - id of the callback
       * @param conversation - conversation where it was added, leave it null for removing from global callbacks
       */
      public synchronized void removeConversationCallback(@NonNull String id, @Nullable String conversation) {
            if (conversation == null) {
                  boolean done = false;
                  Iterator<HashMap<String, Callback<ConversationMessage>>> iter = this.allConversationCallbacks.iterator();
                  while(iter.hasNext() && !done) {
                        HashMap<String, Callback<ConversationMessage>> current = iter.next();
                        if (current.containsKey(id)) {
                              iter.remove();
                              done = true;
                        }
                  }
            } else {
                  ArrayList<HashMap<String, Callback<ConversationMessage>>> list = this.conversationCallbacksMap.get(conversation);
                  if (list == null)
                        return;

                  boolean done = false;
                  Iterator<HashMap<String, Callback<ConversationMessage>>> iter = list.iterator();
                  while (iter.hasNext() && !done) {
                        HashMap<String, Callback<ConversationMessage>> current = iter.next();
                        if (current.containsKey(id)) {
                              iter.remove();
                              done = true;
                        }
                  }

                  if (done)
                        this.conversationCallbacksMap.put(conversation, list);
            }
      }

      /**
       * Removes a callback prevously added to the Message Success callbacks
       *
       * @param id - id of the callback
       * @param conversation - conversation this callback was linked to
       */
      public synchronized void removeMessageSuccessCallback(@NonNull String id, @NonNull String conversation) {
            ArrayList<HashMap<String, Callback<ConversationMessage>>> list = this.successCallbacksMap.get(conversation);
            if (list == null)
                  return;

            boolean done = false;
            Iterator<HashMap<String, Callback<ConversationMessage>>> iter = list.iterator();
            while (iter.hasNext() && !done) {
                  HashMap<String, Callback<ConversationMessage>> current = iter.next();
                  if (current.containsKey(id)) {
                        iter.remove();
                        done = true;
                  }
            }

            if (done)
                  this.successCallbacksMap.put(conversation, list);
      }

      public synchronized ConversationMessage getConversationMessages(long timestamp, short type) {
            return conversationDao.getUniqueMessage(timestamp, type);
      }

      public synchronized List<ConversationMessage> getConversationMessages(String c) {
            return conversationDao.getMessagesFromConversation(c);
      }

      public synchronized ConversationMessage getLastConversationMessage(String conversation) {
            return conversationDao.getLastMessageFromConversation(conversation);
      }

      public synchronized List<ConversationMessage> getUnsuccessMessages() {
            return conversationDao.getUnsuccess();
      }

      public synchronized void setMessageAsSuccess(ConversationMessage msg) {
            this.conversationDao.setAsSuccess(msg.timestamp);

            //Do the callbacks
            ArrayList<HashMap<String, Callback<ConversationMessage>>> callbacks = this.successCallbacksMap.get(msg.conversation);
            if (callbacks == null)
                  return;

            ArrayList<Callback<ConversationMessage>> finalCallback = new ArrayList<>();
            callbacks.forEach((entry) -> entry.forEach((id, callback) -> finalCallback.add(callback)));

            doCallback(msg, finalCallback);
      }

      public synchronized void removeConversationMessage(ConversationMessage msg) {
            this.conversationDao.removeConversationMessage(msg.timestamp, msg.messageType);
      }

      public synchronized void insertContact(@NonNull Contact contact) {
            this.contactDao.insert(contact);
      }

      public synchronized List<Contact> getContacts() {
            return this.contactDao.getInactive();
      }

      public synchronized Contact getContact(String username) {
            return this.contactDao.getContact(username);
      }

      public synchronized List<Contact> getChats() {
            return this.contactDao.getActive();
      }

      public synchronized void setContactUnread(String username, boolean value) {
            this.contactDao.setUnread(username, value);
      }

      public synchronized void setNewUserSuccessCallback(Callback<Credential> callback) {
            this.newUserCallback = callback;
      }

      public synchronized void createCredentials(Credential credential) {
            this.credentialsDao.insert(credential);
            if (newUserCallback != null)
                  doCallback(credential, new ArrayList<>(Collections.singletonList(newUserCallback)));
      }

      public synchronized Credential getCredentials() {
            return this.credentialsDao.getCredentials();
      }

      public synchronized void changeIpAddress(String ipAddress) {
            this.credentialsDao.changeIpAddress(ipAddress);
      }
}
