package com.bichoapp.pos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

/**
 * Impressão térmica para Golink V1 (IposPrinter via Bluetooth)
 * Usa a biblioteca dantsu/ESCPOS-ThermalPrinter-Android (mesma do app anterior)
 *
 * Configuração: endereço MAC da impressora salvo no SQLite (campo "impressora")
 * Golink V1 — IposPrinter MAC: configurado pelo usuário nas configurações do app
 */
public class PrinterHelper {

    private static final String TAG = "BichoAppPrinter";

    // Largura em caracteres da impressora 58mm do Golink V1
    private static final int CHARS_PER_LINE = 32;
    // DPI padrão impressoras térmicas 58mm
    private static final int PRINTER_DPI = 203;
    // Largura em mm do papel 58mm (área útil ~48mm)
    private static final float PAPER_WIDTH_MM = 48f;

    private final Context context;
    private final String bluetoothAddress;

    public PrinterHelper(Context context, String bluetoothAddress) {
        this.context = context;
        this.bluetoothAddress = (bluetoothAddress != null) ? bluetoothAddress.trim() : "";
    }

    /**
     * Imprime texto formatado.
     * Chamado pelo JavaScript: Android.print("texto")
     */
    public boolean printText(String text) {
        if (bluetoothAddress.isEmpty()) {
            Log.e(TAG, "Endereço Bluetooth não configurado. Configure nas configurações do app.");
            return false;
        }

        try {
            BluetoothConnection connection = new BluetoothConnection(
                android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                    .getRemoteDevice(bluetoothAddress)
            );
            return printWithConnection(connection, buildHeader() + convertToEscPosFormat(text), false);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao imprimir: " + e.getMessage());
            return false;
        }
    }

    /**
     * Imprime usando a primeira impressora Bluetooth pareada.
     * Fallback quando endereço não configurado.
     */
    public boolean printTextFirstPaired(String text) {
        try {
            BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
            if (connection == null) {
                Log.e(TAG, "Nenhuma impressora Bluetooth pareada encontrada.");
                return false;
            }

            return printWithConnection(connection, buildHeader() + convertToEscPosFormat(text), false);

        } catch (Exception e) {
            Log.e(TAG, "Erro ao imprimir (primeira pareada): " + e.getMessage());
            return false;
        }
    }

    public boolean printFormattedReceipt(String formattedText) {
        if (bluetoothAddress.isEmpty()) {
            Log.e(TAG, "Endereço Bluetooth não configurado. Configure nas configurações do app.");
            return false;
        }

        try {
            BluetoothConnection connection = new BluetoothConnection(
                android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                    .getRemoteDevice(bluetoothAddress)
            );
            return printWithConnection(connection, formattedText, true);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao preparar impressão formatada: " + e.getMessage());
            return false;
        }
    }

    public boolean printFormattedReceiptFirstPaired(String formattedText) {
        try {
            BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
            if (connection == null) {
                Log.e(TAG, "Nenhuma impressora Bluetooth pareada encontrada.");
                return false;
            }
            return printWithConnection(connection, formattedText, true);
        } catch (Exception e) {
            Log.e(TAG, "Erro na impressão formatada pela primeira pareada: " + e.getMessage());
            return false;
        }
    }

    private boolean printWithConnection(
            BluetoothConnection connection,
            String formattedText,
            boolean includeLogo
    ) {
        EscPosPrinter printer = null;
        try {
            printer = new EscPosPrinter(connection, PRINTER_DPI, PAPER_WIDTH_MM, CHARS_PER_LINE);
            String logo = includeLogo ? buildLogo(printer) : "";
            printer.printFormattedTextAndCut(logo + formattedText);
            Log.d(TAG, "Impressão ESC/POS concluída.");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erro durante impressão ESC/POS: " + e.getMessage());
            return false;
        } finally {
            if (printer != null) {
                try { printer.disconnectPrinter(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Monta o cabeçalho da impressão com nome do app centralizado e em negrito.
     */
    private String buildHeader() {
        return "[C]<b>BichoApp</b>\n";
    }

    private String buildLogo(EscPosPrinter printer) {
        Bitmap original = null;
        Bitmap scaled = null;
        Bitmap withBg = null;
        try {
            original = BitmapFactory.decodeResource(context.getResources(), R.drawable.mascote);
            if (original == null) return "";

            int width = 128;
            int height = Math.max(1, Math.round(original.getHeight() * (width / (float) original.getWidth())));
            scaled = Bitmap.createScaledBitmap(original, width, height, true);

            // Compor sobre fundo branco para evitar fundo preto na impressão térmica
            withBg = Bitmap.createBitmap(scaled.getWidth(), scaled.getHeight(), Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(withBg);
            canvas.drawColor(android.graphics.Color.WHITE);
            canvas.drawBitmap(scaled, 0, 0, null);

            String hexadecimal = PrinterTextParserImg.bitmapToHexadecimalString(printer, withBg, false);
            return hexadecimal.isEmpty() ? "" : "[C]<img>" + hexadecimal + "</img>\n";
        } catch (Exception e) {
            Log.w(TAG, "Logo não suportado; usando cabeçalho textual: " + e.getMessage());
            return "";
        } finally {
            if (withBg != null) withBg.recycle();
            if (scaled != null && scaled != original) scaled.recycle();
            if (original != null) original.recycle();
        }
    }

    /**
     * Converte texto da tela para formato ESC/POS da biblioteca dantsu.
     * Reproduz o layout visual da pule: separadores, negrito, colunas alinhadas.
     */
    private String convertToEscPosFormat(String text) {
        if (text == null || text.isEmpty()) return "[L]\n";

        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();

            // Linha vazia
            if (trimmed.isEmpty()) {
                sb.append("[L]\n");
                continue;
            }

            // Separadores tracejados (---) ou duplos (===) → imprime como está
            if (trimmed.matches("[-]{3,}") || trimmed.matches("[=]{3,}")) {
                sb.append("[L]").append(trimmed).append("\n");
                continue;
            }

            // Pula "BichoApp" do texto (já impresso pelo buildHeader com mascote)
            if (trimmed.equalsIgnoreCase("BichoApp") || trimmed.equalsIgnoreCase("Bicho App")) {
                continue;
            }

            // TOTAL: → negrito com valor alinhado à direita
            if (trimmed.startsWith("TOTAL:")) {
                String valor = extrairValor(trimmed);
                if (valor != null) {
                    sb.append("[L]<b>TOTAL:</b>[R]<b>").append(valor).append("</b>\n");
                } else {
                    sb.append("[L]<b>").append(trimmed).append("</b>\n");
                }
                continue;
            }

            // Linhas de categoria em maiúsculas (GRUPO, MILHAR, CENTENA, DEZENA...) → negrito
            if (trimmed.equals(trimmed.toUpperCase())
                    && trimmed.length() > 4
                    && !trimmed.matches("[\\d\\s\\-\\.\\,R\\$]+")) {
                sb.append("[L]<b>").append(trimmed).append("</b>\n");
                continue;
            }

            // Valor e Prêmio → chave à esquerda, R$ à direita
            if (trimmed.contains("R$")) {
                int idx = trimmed.lastIndexOf("R$");
                String chave = trimmed.substring(0, idx).trim();
                String valor = trimmed.substring(idx).trim();
                if (!chave.isEmpty()) {
                    sb.append("[L]").append(chave).append("[R]").append(valor).append("\n");
                    continue;
                }
            }

            // Linhas com chave: valor simples (PULE, Data, Cambista...)
            if (trimmed.contains(":") && !trimmed.startsWith("http")) {
                int colon = trimmed.indexOf(":");
                String chave = trimmed.substring(0, colon + 1).trim();
                String valor = trimmed.substring(colon + 1).trim();
                if (!valor.isEmpty() && chave.length() <= 20) {
                    sb.append("[L]").append(chave).append(" ").append(valor).append("\n");
                    continue;
                }
            }

            // Linha padrão à esquerda
            sb.append("[L]").append(trimmed).append("\n");
        }

        return sb.toString();
    }

    /** Extrai "R$ XX,XX" do final de uma linha */
    private String extrairValor(String linha) {
        int idx = linha.lastIndexOf("R$");
        if (idx >= 0) return linha.substring(idx).trim();
        return null;
    }
}
