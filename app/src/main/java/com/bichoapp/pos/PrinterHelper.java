package com.bichoapp.pos;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Impressão térmica para Golink V1 (impressora built-in 58mm)
 * Tenta 3 métodos na ordem:
 * 1. Porta serial interna (built-in printer via /dev/ttyS*)
 * 2. Bluetooth ESC/POS (impressora externa)
 * 3. Log de erro se ambos falharem
 */
public class PrinterHelper {

    private static final String TAG = "BichoAppPrinter";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Portas seriais comuns em terminais POS Android genéricos
    private static final String[] SERIAL_PORTS = {
        "/dev/ttyS1", "/dev/ttyS2", "/dev/ttyS0",
        "/dev/ttyMT1", "/dev/ttyMT2",
        "/dev/printer"
    };

    private final Context context;
    private final String bluetoothAddress;

    // Comandos ESC/POS básicos
    private static final byte[] CMD_INIT        = {0x1B, 0x40};           // ESC @ - inicializa
    private static final byte[] CMD_ALIGN_CENTER= {0x1B, 0x61, 0x01};    // ESC a 1 - centraliza
    private static final byte[] CMD_ALIGN_LEFT  = {0x1B, 0x61, 0x00};    // ESC a 0 - esquerda
    private static final byte[] CMD_BOLD_ON     = {0x1B, 0x45, 0x01};    // ESC E 1 - negrito
    private static final byte[] CMD_BOLD_OFF    = {0x1B, 0x45, 0x00};    // ESC E 0 - sem negrito
    private static final byte[] CMD_FEED_CUT    = {0x1B, 0x64, 0x05, 0x1D, 0x56, 0x42, 0x00}; // 5 linhas + corte parcial
    private static final byte[] CMD_LF          = {0x0A};                 // line feed

    public PrinterHelper(Context context, String bluetoothAddress) {
        this.context = context;
        this.bluetoothAddress = bluetoothAddress;
    }

    /**
     * Imprime texto simples formatado
     * Chamado pelo JavaScript: Android.print(texto)
     * Ordem: Bluetooth primeiro (se configurado), depois serial interna
     */
    public boolean printText(String text) {
        byte[] data = buildPrintData(text);

        // Bluetooth primeiro quando endereço configurado (IposPrinter no Golink V1)
        if (bluetoothAddress != null && !bluetoothAddress.isEmpty()) {
            if (printToBluetooth(data)) {
                Log.d(TAG, "Imprimiu via Bluetooth: " + bluetoothAddress);
                return true;
            }
            Log.w(TAG, "Bluetooth falhou, tentando serial...");
        }

        // Fallback: porta serial interna
        for (String port : SERIAL_PORTS) {
            if (printToSerial(port, data)) {
                Log.d(TAG, "Imprimiu via serial: " + port);
                return true;
            }
        }

        Log.e(TAG, "Falha ao imprimir: nenhum método funcionou");
        return false;
    }

    /**
     * Imprime bytes ESC/POS raw (recebidos como array de inteiros do JS)
     * Chamado pelo JavaScript: Android.printRaw([...bytes])
     */
    public boolean printRaw(byte[] data) {
        for (String port : SERIAL_PORTS) {
            if (printToSerial(port, data)) return true;
        }
        if (bluetoothAddress != null && !bluetoothAddress.isEmpty()) {
            return printToBluetooth(data);
        }
        return false;
    }

    private byte[] buildPrintData(String text) {
        try {
            byte[] textBytes = text.getBytes("UTF-8");
            byte[] result = new byte[
                CMD_INIT.length +
                CMD_ALIGN_LEFT.length +
                textBytes.length +
                CMD_FEED_CUT.length
            ];
            int pos = 0;
            System.arraycopy(CMD_INIT,       0, result, pos, CMD_INIT.length);       pos += CMD_INIT.length;
            System.arraycopy(CMD_ALIGN_LEFT, 0, result, pos, CMD_ALIGN_LEFT.length); pos += CMD_ALIGN_LEFT.length;
            System.arraycopy(textBytes,      0, result, pos, textBytes.length);       pos += textBytes.length;
            System.arraycopy(CMD_FEED_CUT,   0, result, pos, CMD_FEED_CUT.length);
            return result;
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private boolean printToSerial(String portPath, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(portPath);
            fos.write(data);
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean printToBluetooth(byte[] data) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) return false;

        BluetoothSocket socket = null;
        try {
            BluetoothDevice device = adapter.getRemoteDevice(bluetoothAddress);
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            adapter.cancelDiscovery();
            socket.connect();
            OutputStream out = socket.getOutputStream();
            out.write(data);
            out.flush();
            Thread.sleep(500); // aguarda impressão
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erro Bluetooth: " + e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
