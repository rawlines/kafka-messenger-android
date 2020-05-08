package com.github.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.R;
import com.github.db.contact.Contact;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

public class NewContactActivity extends AppCompatActivity {
      private class ContactDataObject {
            String username;
            String publicKey;
      }

      private static class SaveContactRunnable implements Runnable {
            private Contact contact;
            private Handler handler;
            SaveContactRunnable(Contact contact, Handler handler) {
                  this.contact = contact;
                  this.handler = handler;
            }
            @Override
            public void run() {
                  MainActivity.databaseManager.insertContact(contact);
                  //TODO: send command to server
                  Message msg = new Message();
                  msg.arg1 = 0;
                  handler.sendMessage(msg);
            }
      }

      private Handler onSavedHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                  if (msg.arg1 == 0) {
                        Toast.makeText(NewContactActivity.this, R.string.toast_okey_key, Toast.LENGTH_LONG).show();
                        finish();
                  } else {
                        //OH no
                  }
            }
      };

      private EditText keyInput;
      private EditText alias;

      @Override
      protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_add_contact);

            FloatingActionButton doneButton = findViewById(R.id.add_contact_done_button);
            Button qrScan = findViewById(R.id.scan_qr_button);
            keyInput = findViewById(R.id.key_text_input);
            alias = findViewById(R.id.alias_text_input);

            keyInput.addTextChangedListener(new TextWatcher() {
                  @Override
                  public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                  @Override
                  public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (s.toString().trim().isEmpty()) doneButton.hide(); else doneButton.show();
                  }

                  @Override
                  public void afterTextChanged(Editable s) { }
            });

            doneButton.setOnClickListener(this::onClickDoneButton);
      }

      private void onClickDoneButton(View v) {
            try {
                  String plain = keyInput.getText().toString();
                  Gson gson = new Gson();
                  ContactDataObject data = gson.fromJson(plain, ContactDataObject.class);
                  saveIntoDB(data);
            } catch (Exception e) {
                  Toast.makeText(this, R.string.toast_wrong_key, Toast.LENGTH_LONG).show();
                  e.printStackTrace();
            }
      }

      private void saveIntoDB(ContactDataObject obj) {
            String s = this.alias.getText().toString().trim();
            String alias = s.isEmpty() ? obj.username : s;
            Contact contact = new Contact(obj.username, alias, obj.publicKey.getBytes(StandardCharsets.ISO_8859_1));
            new Thread(new SaveContactRunnable(contact, onSavedHandler)).start();
      }
}
