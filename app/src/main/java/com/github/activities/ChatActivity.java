package com.github.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.MainActivity;
import com.github.R;
import com.github.ui.adapters.ConversationRecyclerViewAdapter;
import com.github.db.conversation.ConversationMessage;
import com.github.db.conversation.ConversationMessage.MetaData;
import com.github.utils.Cryptography;
import com.github.utils.NetworkQueues;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import static com.github.db.conversation.ConversationMessage.SENT;

public class ChatActivity extends AppCompatActivity {
      private String conversation;

      private RecyclerView recyclerView;
      private ConversationRecyclerViewAdapter mAdapter;
      private LinearLayoutManager layoutManager;

      private Runnable backgroundDBMessageFetcher = () -> {
            List<ConversationMessage> messages =
                  MainActivity.databaseManager.getConversationMessages(conversation);

            mAdapter.addMessages(messages);
      };

      private class MessageSenderThread implements Runnable {
            private ConversationMessage msg;

            public MessageSenderThread(ConversationMessage msg) {
                  this.msg = msg;
            }

            @Override
            public void run() {
                  try {
                        MainActivity.databaseManager.insertConversationMessage(msg);
                        NetworkQueues.producedWithoutAck.add(msg);
                        MainActivity.publicWriter.sendPROD(msg.conversation, msg.content);
                  } catch (Exception ignored) {}
            }
      }

      ;

      @Override
      protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_conversation);

            this.conversation = MainActivity.currentConversation;
            getSupportActionBar().setTitle(conversation);

            initChatGUI();
            new Thread(backgroundDBMessageFetcher).start();

            //Add the database listener
            MainActivity.databaseManager.addConversationCallback(conversation,
                  this::databaseListener);
            MainActivity.databaseManager.addSuccessCallback(conversation,
                  this::successListener);
      }

      @Override
      protected void onDestroy() {
            super.onDestroy();
            MainActivity.currentConversation = null;
      }

      public void initChatGUI() {
            recyclerView = findViewById(R.id.conversation_recyclerview);
            layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true);
            recyclerView.setLayoutManager(layoutManager);
            mAdapter = new ConversationRecyclerViewAdapter();

            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(mAdapter);

            FloatingActionButton fab = findViewById(R.id.send_message_button);
            fab.setOnClickListener(this::sendButtonListener);
      }

      public void sendButtonListener(View v) {
            TextView tv = findViewById(R.id.message_input);
            String s = tv.getText().toString().trim();
            if (s.isEmpty())
                  return;


            MetaData md = new MetaData();
            md.timestamp = System.currentTimeMillis();
            md.plain = s;
            md.source = MainActivity.username;

            ConversationMessage msg = ConversationMessage.fromCryptedBytes(Cryptography.metadataToCryptedBytes(md));
            msg.messageType = SENT;
            msg.conversation = conversation;

            new Thread(new MessageSenderThread(msg)).start();

            tv.setText("");
      }

      public void databaseListener(ConversationMessage msg) {
            mAdapter.addMessage(msg);
            recyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
      }

      public void successListener(ConversationMessage msg) {
            mAdapter.setAsSuccess(msg);
      }
}
