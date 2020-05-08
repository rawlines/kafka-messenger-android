package com.github.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.R;
import com.github.db.credentials.Credential;
import com.github.utils.PublicWriter;
import com.github.utils.SSLFactory;
import com.rest.commonutils.InputChecker;
import com.rest.net.AcknPacket;
import com.rest.net.Packet;
import com.rest.net.PacketReader;
import com.rest.net.PacketWriter;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static com.rest.net.Packet.PacketType.ACKN;
import static com.rest.net.Packet.PacketType.CREA;

public class CredentialsActivity extends AppCompatActivity {
      private TextView usernameView;
      private TextView passwordView;
      private CheckBox registrationCheck;
      private Button loginButton;
      private Spinner serverSelector;

      private SSLSocket socket;

      private class WaitForAckRunnable implements Runnable {
            private Credential creds;
            WaitForAckRunnable(Credential creds) { this.creds = creds; }
            public void run() {
                  try {
                        String[] splitted = creds.ipAddress.split(":");
                        SSLSocketFactory factory = SSLFactory.getSSLFactory(MainActivity.trustStore, MainActivity.keyStore);
                        socket = (SSLSocket) factory.createSocket(splitted[0], Integer.parseInt(splitted[1]));

                        PublicWriter publicWriter = new PublicWriter(new PacketWriter(socket.getOutputStream()));
                        PacketReader pReader = new PacketReader(socket.getInputStream());

                        publicWriter.sendCREA(creds.username, creds.password);

                        Packet p = pReader.readPacket();

                        if (p.getPacketType() == ACKN) {
                              AcknPacket ap = (AcknPacket) p;
                              if (ap.getCommand() == CREA) {
                                    MainActivity.databaseManager.createCredentials(creds);
                              }
                        }
                  } catch (Exception e) { }
            }
      }

      private class SaveOnDbRunnable implements Runnable {
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

            try {
                  if (register)
                        newUser(user, pass, ip);
                  else
                        login(user, pass, ip);

            } catch (IOException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                  Toast.makeText(this,"Ha ocurrido un error " + e.getMessage(), Toast.LENGTH_SHORT ).show();
            }
      }

      private void login(String user, String pass, String ip) {
            Credential creds = new Credential(user, pass, ip);
            new Thread(new SaveOnDbRunnable(creds)).start();
      }

      private void newUser(String user, String pass, String ip) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
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
