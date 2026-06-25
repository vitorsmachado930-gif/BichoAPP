package com.bichoapp.pos;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class AndroidBridge {

    private static final String TAG = "AndroidBridge";

    private final Context context;
    private final PrinterHelper printer;
    private final Runnable onOpenConfig;

    public AndroidBridge(Context context, PrinterHelper printer, Runnable onOpenConfig) {
        this.context = context;
        this.printer = printer;
        this.onOpenConfig = onOpenConfig;
    }

    @JavascriptInterface
    public boolean print(String text) {
        Log.d(TAG, "print() chamado, texto: " + text.length() + " chars");
        showToast("🖨️ Enviando para impressora...");
        boolean result = printer.printText(text);
        if (result) {
            showToast("✅ Impressão concluída");
        } else {
            showToast("❌ Falha na impressão. Verifique Bluetooth e endereço da impressora.");
        }
        return result;
    }

    @JavascriptInterface
    public boolean printBase64(String base64) {
        try {
            byte[] data = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            return printer.printRaw(data);
        } catch (Exception e) {
            Log.e(TAG, "Erro base64: " + e.getMessage());
            return false;
        }
    }

    @JavascriptInterface
    public void openConfig() {
        if (onOpenConfig != null) {
            ((Activity) context).runOnUiThread(onOpenConfig);
        }
    }

    @JavascriptInterface
    public String getVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    @JavascriptInterface
    public boolean isNativeApp() {
        return true;
    }

    private void showToast(final String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        );
    }
}
