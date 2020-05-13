package com.github.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.R;

public class ShareMyCodeActivity extends AppCompatActivity {
      private String code;

      @Override
      protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_share_mycode);

            code = getIntent().getStringExtra("sharecode");

            EditText text = findViewById(R.id.shared_code);
            Button copy = findViewById(R.id.copy_code_button);

            text.setText(code);
            copy.setOnClickListener(this::onCopy);
      }

      private void onCopy(View v) {
            ClipboardManager clipManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData data = ClipData.newPlainText("kafkamessenger", code);
            clipManager.setPrimaryClip(data);
            Toast.makeText(this, R.string.successfully_copied, Toast.LENGTH_SHORT).show();
      }
}
