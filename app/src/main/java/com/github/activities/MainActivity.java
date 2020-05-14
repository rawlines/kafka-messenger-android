package com.github.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.R;
import com.github.crypto.Cryptography;
import com.github.db.AppDatabase;
import com.github.db.credentials.Credential;
import com.github.ui.adapters.MainTabsPagerAdapter;
import com.github.db.DatabaseManager;
import com.github.utils.KeyStoreUtil;
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;

public class MainActivity extends AppCompatActivity {
      public static KeyStore trustStore;
      public static KeyStore keyStore;

      public static byte[] myPublicKey;

      public static Credential globalCredentials = null;

      public static String currentConversation = null;

      public static PublicWriter publicWriter;
      public static DatabaseManager databaseManager;

      private final String KEYSTORE_FILENAME = "client.bks";

      private final Thread mainListenerThread = new MainListenerThread(this);

      public MenuItem deleteMenuButton;

      Handler credentialsHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                  //if BAD
                  if (msg.arg1 == 1) {
                        Intent intent = new Intent(MainActivity.this, CredentialsActivity.class);
                        startActivityForResult(intent, CredentialsActivity.NEW_SESSION_CODE);
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
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            initKeyStores();
            initDatabase();
            fetchCredentialsThread.start();
      }

      /**
       * Handle the result of the {@link CredentialsActivity}
       *
       * @param requestCode - Handle if are new credentials or modifying existing
       * @param resultCode -
       * @param data -
       */
      @Override
      protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (requestCode == CredentialsActivity.NEW_SESSION_CODE && (data == null || resultCode != RESULT_OK)) {
                  finish();
                  return;
            }

            if (requestCode == CredentialsActivity.CHANGE_CREDS_CODE) {
                  //Unlock the listener thread
                  synchronized (mainListenerThread) {
                        mainListenerThread.notify();
                  }
                  if (data == null || resultCode != RESULT_OK)
                        return;
            }

            String username = data.getStringExtra("username");
            String password = data.getStringExtra("password");
            String ipAddress = data.getStringExtra("ip");

            globalCredentials = new Credential(username, password, ipAddress);

            if (requestCode != CredentialsActivity.CHANGE_CREDS_CODE)
                 initGUI();
      }

      @Override
      public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            deleteMenuButton = menu.findItem(R.id.delete_menu_mainactivity);
            return super.onCreateOptionsMenu(menu);
      }

      @Override
      public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                  case R.id.change_server_option:
                        changeServerRoutine();
                        break;
                  case R.id.share_code_option:
                        shareCodeRoutine();
                        break;
            }

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


      /**
       * initialize keystores for SSL connection
       */
      private void initKeyStores() {
            try {
                  trustStore = KeyStore.getInstance("BKS");
                  trustStore.load(getResources().openRawResource(R.raw.kafkarootca), "123456".toCharArray());

                  Log.d("CERT", getFilesDir().getAbsolutePath() + "/" + KEYSTORE_FILENAME);
                  //Generate or init keystore
                  keyStore = KeyStoreUtil.getKeyStore(
                        new File(getFilesDir().getAbsolutePath() + "/" + KEYSTORE_FILENAME), "123456");
                  Cryptography.setPrivKey((PrivateKey) keyStore.getKey("default", "123456".toCharArray()));

                  myPublicKey = Base64.encode(keyStore.getCertificate("default").getPublicKey().getEncoded(), Base64.DEFAULT);
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
            this.mainListenerThread.start();
      }

      /**
       * Routine for displaying the dialog with available servers and allowing the user to perform the selection
       */
      public void changeServerRoutine() {
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
                  .setSingleChoiceItems(servers, selectedIndx, (dialog, which) ->
                        globalCredentials.ipAddress = servers[which]
                  ).setPositiveButton(R.string.server_selector_ok, (dialog, which) ->
                  new Thread(() -> {
                        MainActivity.databaseManager.changeIpAddress(globalCredentials.ipAddress);
                        handler.sendMessage(new Message());
                  }).start()
            ).show();
      }

      /**
       * Routine for displaying the dialog with
       */
      private void shareCodeRoutine() {
            final String jsonTemplate = "{\"username\":\"%s\",\"publicKey\":\"%s\"}";
            try {
                  String formatted = String.format(jsonTemplate, globalCredentials.username, new String(myPublicKey, StandardCharsets.ISO_8859_1));

                  Intent intent = new Intent(this, ShareMyCodeActivity.class);
                  intent.putExtra("sharecode", formatted);
                  startActivity(intent);
            } catch (Exception e) {
                  e.printStackTrace();
                  Toast.makeText(this, "Ha ocurido un error al compartir el código", Toast.LENGTH_SHORT).show();
            }
      }
}