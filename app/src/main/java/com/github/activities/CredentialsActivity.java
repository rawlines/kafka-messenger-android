package com.github.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.R;

public class CredentialsActivity extends AppCompatActivity {
      @Override
      protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_credentials);

            Spinner serverSelector = findViewById(R.id.server_selector);

            ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.server_list, R.layout.spinner_textview);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_textview);
            serverSelector.setAdapter(adapter);

            TextView usernameView = findViewById(R.id.username_input);
            TextView passwordView = findViewById(R.id.password_input);
            CheckBox registrationCheck = findViewById(R.id.register_checkbox);
            Button loginButton = findViewById(R.id.login_buton);

            loginButton.setOnClickListener(this::onClick);
      }

      private void onClick(View v) {
            //Perform login o registration

            Intent data = new Intent();
            data.putExtra("username", "");
            data.putExtra("password", "");
            setResult(RESULT_OK, data);
            finish();
      }
}
