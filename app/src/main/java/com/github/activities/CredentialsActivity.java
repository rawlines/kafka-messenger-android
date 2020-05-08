package com.github.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.R;
import com.github.db.credentials.Credential;
import com.github.utils.PublicWriter;
import com.github.utils.SSLFactory;
import com.rest.commonutils.InputChecker;
import com.rest.net.AcknPacket;
import com.rest.net.DenyPacket;
import com.rest.net.Packet;
import com.rest.net.PacketReader;
import com.rest.net.PacketWriter;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static com.rest.net.Packet.PacketType.CREA;

public class CredentialsActivity extends AppCompatActivity {
      private TextView usernameView;
      private TextView passwordView;
      private CheckBox registrationCheck;
      private Button loginButton;
      private Spinner serverSelector;
      private ProgressBar registrationProgressBar;

      private SSLSocket socket;

      private final Handler denyRegistrationHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                  String error = msg.getData().getString("error");

                  registrationProgressBar.setVisibility(View.INVISIBLE);
                  loginButton.setEnabled(true);
                  Toast.makeText(CredentialsActivity.this,
                        error, Toast.LENGTH_LONG)
                        .show();
            }
      };

      private class WaitForAckRunnable implements Runnable {
            private Credential creds;
            WaitForAckRunnable(Credential creds) { this.creds = creds; }
            public void run() {
                  Message msg = new Message();
                  Bundle b = new Bundle();
                  msg.setData(b);

                  try {
                        String[] splitted = creds.ipAddress.split(":");
                        SSLSocketFactory factory = SSLFactory.getSSLFactory(MainActivity.trustStore, MainActivity.keyStore);
                        socket = (SSLSocket) factory.createSocket(splitted[0], Integer.parseInt(splitted[1]));

                        PublicWriter publicWriter = new PublicWriter(new PacketWriter(socket.getOutputStream()));
                        PacketReader pReader = new PacketReader(socket.getInputStream());

                        publicWriter.sendCREA(creds.username, creds.password);

                        Packet p = pReader.readPacket();

                        switch (p.getPacketType()) {
                              case ACKN:
                                    AcknPacket ap = (AcknPacket) p;
                                    if (ap.getCommand() == CREA)
                                          new Thread(new SaveOnDbRunnable(creds)).start();

                                    break;

                              case DENY:
                                    DenyPacket dp = (DenyPacket) p;
                                    if (dp.getCommand() == CREA) {
                                          b.putString("error", getText(R.string.rejected_registration).toString());
                                          denyRegistrationHandler.sendMessage(msg);
                                    }

                                    break;
                        }
                  } catch (Exception e) {
                        b.putString("error", getText(R.string.general_error).toString() + e.getMessage());
                        denyRegistrationHandler.sendMessage(msg);
                  }
            }
      }

      private static class SaveOnDbRunnable implements Runnable {
            Credential cred;
            SaveOnDbRunnable(Credential cred) { this.cred = cred; }
            @Override
            public void run() {
                  MainActivity.databaseManager.createCredentials(cred);
            }
      }

      @Override
      protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_credentials);

            serverSelector = findViewById(R.id.server_selector);

            ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.server_list, R.layout.spinner_textview);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_textview);
            serverSelector.setAdapter(adapter);

            usernameView = findViewById(R.id.username_input);
            passwordView = findViewById(R.id.password_input);
            registrationCheck = findViewById(R.id.register_checkbox);
            loginButton = findViewById(R.id.login_buton);
            registrationProgressBar = findViewById(R.id.registration_progress_bar);

            loginButton.setOnClickListener(this::onClick);
            MainActivity.databaseManager.setNewUserSuccessCallback(this::onSuccess);
      }

      private void onClick(View v) {
            boolean register = registrationCheck.isChecked();

            String user = usernameView.getText().toString();
            String pass = passwordView.getText().toString();
            String ip = serverSelector.getSelectedItem().toString();

            if (!InputChecker.isValidUsername(user) || !InputChecker.isValidPassword(pass))
                  return;

            if (register)
                  newUser(user, pass, ip);
            else
                  login(user, pass, ip);
      }

      private void login(String user, String pass, String ip) {
            Credential creds = new Credential(user, pass, ip);
            new Thread(new SaveOnDbRunnable(creds)).start();
      }

      private void newUser(String user, String pass, String ip) {
            registrationProgressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);

            Credential creds = new Credential(user, pass, ip);
            new Thread(new WaitForAckRunnable(creds)).start();
      }

      private void onSuccess(Credential cred) {
            try {
                  socket.close();
            } catch (Exception ignored) {}

            Intent data = new Intent();
            data.putExtra("username", cred.username);
            data.putExtra("password", cred.password);
            data.putExtra("ip", cred.ipAddress);
            setResult(RESULT_OK, data);
            finish();
      }
}
