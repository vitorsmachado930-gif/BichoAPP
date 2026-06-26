package com.bichoapp.pos;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class ConfigActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private EditText editUrl;
    private EditText editPrinter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle("Configurações BichoApp POS");
        }

        db = new DatabaseHelper(this);
        editUrl     = findViewById(R.id.edit_url);
        editPrinter = findViewById(R.id.edit_printer);
        Button btnSave   = findViewById(R.id.btn_save);

        // Força texto preto em todos os campos (independente do tema do device)
        editUrl.setTextColor(Color.BLACK);
        editUrl.setHintTextColor(Color.GRAY);
        editPrinter.setTextColor(Color.BLACK);
        editPrinter.setHintTextColor(Color.GRAY);
        editPrinter.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // Preenche com valores atuais
        editUrl.setText(db.getUrl());
        editPrinter.setText(db.getPrinter());

        btnSave.setOnClickListener(v -> {
            String url = editUrl.getText().toString().trim();
            String printer = editPrinter.getText().toString().trim();

            if (url.isEmpty()) {
                Toast.makeText(this, "URL não pode ser vazia", Toast.LENGTH_SHORT).show();
                return;
            }

            // Garante https:// no início
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            db.setUrl(url);
            db.setPrinter(printer);

            Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
