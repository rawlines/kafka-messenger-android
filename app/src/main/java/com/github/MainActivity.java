package com.github;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.github.activities.NewContactActivity;
import com.github.db.AppDatabase;
import com.github.ui.adapters.MainTabsPagerAdapter;
import com.github.db.DatabaseManager;
import com.github.utils.PublicWriter;
import com.github.utils.threads.MainListenerThread;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import androidx.room.Room;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.security.KeyStore;

public class MainActivity extends AppCompatActivity {
      public static String username = "client1";
      public static String password = "1234567890qw";

      public static String currentConversation = null;

      public static PublicWriter publicWriter;
      public static DatabaseManager databaseManager;

      @Override
      protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

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
                        if (tab.getText().toString().equals(getResources().getString(R.string.tab_chats))) {
                              addContacFAB.hide();
                        } else {
                              addContacFAB.show();
                        }
                  }

                  @Override
                  public void onTabUnselected(TabLayout.Tab tab) { }

                  @Override
                  public void onTabReselected(TabLayout.Tab tab) { }
            });
            addContacFAB.setOnClickListener((v) -> {
                  startActivity(new Intent(this, NewContactActivity.class));
            });

            initDatabase();
            initMainListenerThread();
      }

      @Override
      public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
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
            try {
                  KeyStore trustStore = KeyStore.getInstance("BKS");
                  trustStore.load(getResources().openRawResource(R.raw.kafkarootca), "123456".toCharArray());

                  KeyStore keyStore = KeyStore.getInstance("BKS");
                  keyStore.load(getResources().openRawResource(R.raw.client), "123456".toCharArray());

                  new MainListenerThread(keyStore, trustStore).start();
            } catch (Exception e) {
                  e.printStackTrace();
            }
      }
}