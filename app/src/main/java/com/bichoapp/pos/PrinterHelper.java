package com.bichoapp.pos;

import android.content.Context;
import android.util.Log;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

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

        EscPosPrinter printer = null;
        try {
            BluetoothConnection connection = new BluetoothConnection(
                android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                    .getRemoteDevice(bluetoothAddress)
            );

            printer = new EscPosPrinter(connection, PRINTER_DPI, PAPER_WIDTH_MM, CHARS_PER_LINE);

            // Converte texto simples para formato dantsu
            String formatted = convertToEscPosFormat(text);
            printer.printFormattedTextAndCut(formatted);

            Log.d(TAG, "Impressão concluída via Bluetooth: " + bluetoothAddress);
            return true;

        } catch (EscPosConnectionException e) {
            Log.e(TAG, "Erro de conexão Bluetooth: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao imprimir: " + e.getMessage());
            return false;
        } finally {
            if (printer != null) {
                try { printer.disconnectPrinter(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Imprime usando a primeira impressora Bluetooth pareada.
     * Fallback quando endereço não configurado.
     */
    public boolean printTextFirstPaired(String text) {
        EscPosPrinter printer = null;
        try {
            BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
            if (connection == null) {
                Log.e(TAG, "Nenhuma impressora Bluetooth pareada encontrada.");
                return false;
            }

            printer = new EscPosPrinter(connection, PRINTER_DPI, PAPER_WIDTH_MM, CHARS_PER_LINE);
            String formatted = convertToEscPosFormat(text);
            printer.printFormattedTextAndCut(formatted);

            Log.d(TAG, "Impressão concluída via primeira impressora pareada.");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Erro ao imprimir (primeira pareada): " + e.getMessage());
            return false;
        } finally {
            if (printer != null) {
                try { printer.disconnectPrinter(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Converte texto simples para o formato de marcação da biblioteca dantsu.
     * O texto vindo do JavaScript é texto puro com quebras de linha.
     */
    private String convertToEscPosFormat(String text) {
        if (text == null || text.isEmpty()) return "[L]\n";

        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                sb.append("[L]\n");
                continue;
            }

            // Linhas de separação (---) → linha cheia
            if (trimmed.matches("[-=*]{3,}")) {
                sb.append("[L]").append(trimmed).append("\n");
                continue;
            }

            // Linhas com ":" no meio podem ser par chave:valor → duas colunas
            if (trimmed.contains(":") && !trimmed.startsWith("http")) {
                int colon = trimmed.indexOf(":");
                String key = trimmed.substring(0, colon + 1).trim();
                String val = trimmed.substring(colon + 1).trim();
                if (!val.isEmpty() && key.length() < 20) {
                    sb.append("[L]").append(key).append("[R]").append(val).append("\n");
                    continue;
                }
            }

            // Linha normal centralizada se curta (título), esquerda se longa
            if (trimmed.length() <= 20 && (
                trimmed.toUpperCase().equals(trimmed) ||
                lines.length > 2 && trimmed.equals(lines[0].trim()))) {
                sb.append("[C]").append(trimmed).append("\n");
            } else {
                sb.append("[L]").append(trimmed).append("\n");
            }
        }

        return sb.toString();
    }
}
