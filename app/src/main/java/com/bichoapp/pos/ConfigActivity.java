package com.bichoapp.pos;

import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class ConfigActivity extends AppCompatActivity {

    private static final String PIN = "1234"; // PIN padrão - pode alterar aqui

    private DatabaseHelper db;
    private EditText editUrl;
    private EditText editPrinter;
    private EditText editPin;
    private boolean authenticated = false;

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
        editPin     = findViewById(R.id.edit_pin);
        Button btnSave   = findViewById(R.id.btn_save);
        Button btnVerify = findViewById(R.id.btn_verify_pin);

        // Campos bloqueados até autenticar
        editUrl.setEnabled(false);
        editPrinter.setEnabled(false);
        btnSave.setEnabled(false);

        // Preenche com valores atuais (mascarados)
        editUrl.setText(db.getUrl());
        editPrinter.setText(db.getPrinter());

        btnVerify.setOnClickListener(v -> {
            String pin = editPin.getText().toString().trim();
            if (pin.equals(PIN)) {
                authenticated = true;
                editUrl.setEnabled(true);
                editPrinter.setEnabled(true);
                btnSave.setEnabled(true);
                editPin.setEnabled(false);
                btnVerify.setEnabled(false);
                Toast.makeText(this, "✓ Autenticado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "PIN incorreto", Toast.LENGTH_SHORT).show();
                editPin.setText("");
            }
        });

        btnSave.setOnClickListener(v -> {
            if (!authenticated) return;

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
