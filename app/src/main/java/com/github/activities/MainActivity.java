package com.github.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.github.R;
import com.github.db.AppDatabase;
import com.github.db.credentials.Credential;
import com.github.ui.adapters.MainTabsPagerAdapter;
import com.github.db.DatabaseManager;
import com.github.utils.PublicWriter;
import com.github.utils.threads.MainListenerThread;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.jakewharton.processphoenix.ProcessPhoenix;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.room.Room;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import java.security.KeyStore;

public class MainActivity extends AppCompatActivity {
      public static KeyStore trustStore;
      public static KeyStore keyStore;

      public static Credential globalCredentials = null;

      public static String currentConversation = null;

      public static PublicWriter publicWriter;
      public static DatabaseManager databaseManager;

      Handler credentialsHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                  //if BAD
                  if (msg.arg1 == 1) {
                        Intent intent = new Intent(MainActivity.this, CredentialsActivity.class);
                        startActivityForResult(intent, 0);
                  } else if (msg.arg1 == 0) {
                        initGUI();
                  }
            }
      };

      private Thread fetchCredentialsThread = new Thread() {
            @Override
            public void run() {
                  globalCredentials = MainActivity.databaseManager.getCredentials();
                  Message msg = new Message();
                  //0 = OK, 1 = BAD
                  msg.arg1 = globalCredentials == null ? 1 : 0;
                  credentialsHandler.sendMessage(msg);
            }
      };

      @Override
      protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(null);
            setContentView(R.layout.activity_main);

            initKeyStores();
            initDatabase();
            fetchCredentialsThread.start();
      }

      @Override
      protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (requestCode != 0 || data == null || resultCode != RESULT_OK) {
                  finish();
                  return;
            }

            String username = data.getStringExtra("username");
            String password = data.getStringExtra("password");
            String ipAddress = data.getStringExtra("ip");

            globalCredentials = new Credential(username, password, ipAddress);
            initGUI();
      }

      @Override
      public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return super.onCreateOptionsMenu(menu);
      }

      @Override
      public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() != R.id.change_server_option)
                  return false;

            String[] servers = getResources().getStringArray(R.array.server_list);
            int selectedIndx = 0;
            boolean done = false;
            while (selectedIndx < servers.length && !done) {
                  if (servers[selectedIndx].equals(globalCredentials.ipAddress))
                        done = true;
                  else
                        selectedIndx++;
            }

            final Handler handler = new Handler(Looper.getMainLooper()) {
                  @Override
                  public void handleMessage(@NonNull Message msg) {
                        ProcessPhoenix.triggerRebirth(MainActivity.this);
                  }
            };

            new AlertDialog.Builder(this)
                  .setTitle(R.string.server_selector_title)
                  .setSingleChoiceItems(servers, selectedIndx, (dialog, which) -> {
                        globalCredentials.ipAddress = servers[which];
                  }).setPositiveButton(R.string.server_selector_ok, (dialog, which) -> {
                        new Thread(() -> {
                              MainActivity.databaseManager.changeIpAddress(globalCredentials.ipAddress);
                              handler.sendMessage(new Message());
                        }).start();
                  }).show();

            return super.onOptionsItemSelected(item);
      }

      private void initGUI() {
            MainTabsPagerAdapter tabsPagerAdapter = new MainTabsPagerAdapter(this, getSupportFragmentManager());
            FloatingActionButton addContacFAB = findViewById(R.id.add_contact_floating_button);

            ViewPager viewPager = findViewById(R.id.view_pager);
            viewPager.setAdapter(tabsPagerAdapter);

            TabLayout tabs = findViewById(R.id.tabs);
            tabs.setupWithViewPager(viewPager);

            setSupportActionBar(findViewById(R.id.toolbar));

            tabs.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
                  @Override
                  public void onTabSelected(TabLayout.Tab tab) {
                        if (tab.getText().toString().equals(getResources().getString(R.string.tab_chats)))
                              addContacFAB.hide();
                        else
                              addContacFAB.show();
                  }
                  @Override
                  public void onTabUnselected(TabLayout.Tab tab) { }
                  @Override
                  public void onTabReselected(TabLayout.Tab tab) { }
            });
            addContacFAB.setOnClickListener((v) ->
                  startActivity(new Intent(this, NewContactActivity.class))
            );

            initMainListenerThread();
      }

      private void initKeyStores() {
            try {
                  trustStore = KeyStore.getInstance("BKS");
                  trustStore.load(getResources().openRawResource(R.raw.kafkarootca), "123456".toCharArray());

                  keyStore = KeyStore.getInstance("BKS");
                  keyStore.load(getResources().openRawResource(R.raw.client), "123456".toCharArray());
            } catch (Exception e) {
                  Log.d("CERT", e.getMessage());
                  e.printStackTrace();
            }
      }

      /**
       * Initialize RoomDatabase shared var for all threads.
       */
      private void initDatabase() {
            databaseManager = DatabaseManager.init(Room.databaseBuilder(getApplicationContext(),
                  AppDatabase.class, "database-name").build());
      }

      /**
       * Initializes the main listener thread with SSL configurations.
       */
      private void initMainListenerThread() {
            new MainListenerThread().start();
      }
}