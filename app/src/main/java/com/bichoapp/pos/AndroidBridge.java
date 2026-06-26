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
     * Tenta MAC configurado; se falhar, tenta primeira impressora Bluetooth pareada.
     */
    @JavascriptInterface
    public boolean print(String text) {
        Log.d(TAG, "print() chamado");
        boolean ok = printer.printText(text);
        if (!ok) {
            Log.w(TAG, "Falhou com MAC configurado, tentando primeira pareada...");
            ok = printer.printTextFirstPaired(text);
        }
        return ok;
    }

    /** Imprime uma pule já diagramada com a sintaxe ESC/POS da biblioteca. */
    @JavascriptInterface
    public boolean printFormatted(String formattedText) {
        Log.d(TAG, "printFormatted() chamado");
        boolean ok = printer.printFormattedReceipt(formattedText);
        if (!ok) {
            Log.w(TAG, "Falhou com MAC configurado, tentando primeira pareada...");
            ok = printer.printFormattedReceiptFirstPaired(formattedText);
        }
        return ok;
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
