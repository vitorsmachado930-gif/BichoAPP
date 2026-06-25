package com.bichoapp.pos;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * Ponte JavaScript → Android
 * No BichoApp web, use: window.Android.print("texto") ou window.Android.openConfig()
 */
public class AndroidBridge {

    private static final String TAG = "AndroidBridge";

    private final Context context;
    private PrinterHelper printer;
    private final Runnable onOpenConfig;

    public AndroidBridge(Context context, PrinterHelper printer, Runnable onOpenConfig) {
        this.context = context;
        this.printer = printer;
        this.onOpenConfig = onOpenConfig;
    }

    /** Atualiza a impressora (chamado após salvar config) */
    public void updatePrinter(PrinterHelper printer) {
        this.printer = printer;
    }

    /**
     * Imprime texto simples.
     * Uso no JS: Android.print("Protocolo: 001\nValor: R$ 5,00\n");
     */
    @JavascriptInterface
    public boolean print(String text) {
        Log.d(TAG, "print() chamado");
        return printer.printText(text);
    }

    /**
     * Imprime bytes ESC/POS em base64.
     * Uso no JS: Android.printBase64("G0BAAAAAA..."); // base64 dos bytes ESC/POS
     */
    @JavascriptInterface
    public boolean printBase64(String base64) {
        try {
            byte[] data = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            return printer.printRaw(data);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao decodificar base64: " + e.getMessage());
            return false;
        }
    }

    /**
     * Abre tela de configurações.
     * Uso no JS: Android.openConfig();
     */
    @JavascriptInterface
    public void openConfig() {
        if (onOpenConfig != null) {
            ((android.app.Activity) context).runOnUiThread(onOpenConfig);
        }
    }

    /**
     * Retorna versão do app.
     * Uso no JS: let v = Android.getVersion();
     */
    @JavascriptInterface
    public String getVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    /**
     * Verifica se está rodando no APK (não no browser).
     * Uso no JS: if (typeof Android !== 'undefined' && Android.isNativeApp()) { ... }
     */
    @JavascriptInterface
    public boolean isNativeApp() {
        return true;
    }
}
