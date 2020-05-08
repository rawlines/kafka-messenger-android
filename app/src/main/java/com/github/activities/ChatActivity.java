package com.github.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.R;
import com.github.ui.adapters.ConversationRecyclerViewAdapter;
import com.github.db.conversation.ConversationMessage;
import com.github.db.conversation.ConversationMessage.MetaData;
import com.github.utils.Cryptography;
import com.github.utils.PublicWriter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import static com.github.db.conversation.ConversationMessage.SENT;

public class ChatActivity extends AppCompatActivity {
      private String conversation;

      private boolean selectionMode = false;
      private List<ConversationMessage> selectedMessages = new ArrayList<>();

      private RecyclerView recyclerView;
      private ConversationRecyclerViewAdapter mAdapter;

      private MenuItem deleteSelectedMessagesButton;

      private final Handler messagePutter = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                  Object obj = msg.getData().getSerializable("messages");
                  ArrayList<ConversationMessage> messages = null;

                  messages = (ArrayList<ConversationMessage>) obj;

                  mAdapter.addMessages(messages);
            }
      };

      private final Runnable backgroundDBMessageFetcher = () -> {
            List<ConversationMessage> messages =
                  MainActivity.databaseManager.getConversationMessages(conversation);

            ArrayList<ConversationMessage> data = new ArrayList<>(messages);

            Bundle b = new Bundle();
            b.putSerializable("messages", data);
            Message msg = new Message();
            msg.setData(b);
            messagePutter.sendMessage(msg);
      };

      private static class MessageSenderRunnable implements Runnable {
            private ConversationMessage msg;

            MessageSenderRunnable(ConversationMessage msg) {
                  this.msg = msg;
            }

            @Override
            public void run() {
                  try {
                        MainActivity.databaseManager.insertConversationMessage(msg);
                        PublicWriter.producedWithoutAck.add(msg);
                        MainActivity.publicWriter.sendPROD(msg.conversation, msg.content);
                  } catch (Exception ignored) {}
            }
      }

      @Override
      protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_conversation);

            setSupportActionBar(findViewById(R.id.conversation_toolbar));
            this.conversation = MainActivity.currentConversation;
            ActionBar ab = getSupportActionBar();
            if (ab != null)
                  ab.setTitle(getIntent().getStringExtra("alias"));

            initChatGUI();
            new Thread(backgroundDBMessageFetcher).start();
      }

      @Override
      public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.chatactivity_menu, menu);
            deleteSelectedMessagesButton = menu.findItem(R.id.remove_selected_menu_option);

            return super.onCreateOptionsMenu(menu);
      }

      @Override
      public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() != R.id.remove_selected_menu_option)
                  return false;

            removeSelectedMessages();

            return super.onOptionsItemSelected(item);
      }

      @Override
      protected void onResume() {
            super.onResume();

            //Add the database listener
            MainActivity.databaseManager.addConversationCallback("conversationListener", conversation,
                  this::databaseListener);
            MainActivity.databaseManager.addMessageSuccessCallback("conversationSuccessListener", conversation,
                  this::successListener);
      }

      @Override
      protected void onPause() {
            super.onPause();

            //Remove the database listeners
            MainActivity.databaseManager.removeConversationCallback("conversationListener", conversation);
            MainActivity.databaseManager.removeMessageSuccessCallback("conversationSuccessListener", conversation);
      }

      @Override
      protected void onDestroy() {
            super.onDestroy();
            MainActivity.currentConversation = null;
      }

      public void initChatGUI() {
            recyclerView = findViewById(R.id.conversation_recyclerview);

            LinearLayoutManager layoutManager;
            layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true);
            recyclerView.setLayoutManager(layoutManager);
            mAdapter = new ConversationRecyclerViewAdapter(this::onLongClick ,this::onClick);

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
            md.source = MainActivity.globalCredentials.username;

            ConversationMessage msg = ConversationMessage.fromCryptedBytes(Cryptography.metadataToCryptedBytes(md));
            msg.messageType = SENT;
            msg.conversation = conversation;

            new Thread(new MessageSenderRunnable(msg)).start();

            tv.setText("");
      }

      public void databaseListener(ConversationMessage msg) {
            mAdapter.addMessage(msg);
            recyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
      }

      public void successListener(ConversationMessage msg) {
            mAdapter.setAsSuccess(msg);
      }

      public boolean onClick(View v, ConversationMessage msg) {
            if (!selectionMode)
                  return false;

            CardView cv = (CardView) v;

            boolean selected = !v.isSelected();

            if (selected) {
                  cv.setSelected(true);
                  cv.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.secondaryDarkColor, getTheme())));
                  selectedMessages.add(msg);
            } else {
                  cv.setSelected(false);
                  cv.setBackgroundTintList(null);
                  selectedMessages.remove(msg);

                  if (selectedMessages.isEmpty())
                        exitSelectionMode();
            }

            return true;
      }

      public boolean onLongClick(View v, ConversationMessage msg) {
            startSelectionMode();
            return false;
      }

      private void startSelectionMode() {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null)
                  vibrator.vibrate(VibrationEffect.createOneShot(50,50));

            selectionMode = true;
            deleteSelectedMessagesButton.setVisible(true);
      }

      private void exitSelectionMode() {
            selectionMode = false;
            deleteSelectedMessagesButton.setVisible(false);
      }

      private void removeSelectedMessages() {
            final Handler handler = new Handler(Looper.getMainLooper()) {
                  @Override
                  public void handleMessage(@NonNull Message msg) {
                        //remove messages from recycler view
                        mAdapter.removeIndexes(selectedMessages);
                        exitSelectionMode();
                        selectedMessages.clear();
                  }
            };

            //Remove messages from the database and then notify it so it will be removed from the
            //recycler view
            new Thread(() -> {
                  selectedMessages.forEach(MainActivity.databaseManager::removeConversationMessage);

                  Message msg = new Message();
                  handler.sendMessage(msg);
            }).start();
      }
}
