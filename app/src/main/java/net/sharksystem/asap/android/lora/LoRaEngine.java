package net.sharksystem.asap.android.lora;

import android.content.Context;
import android.util.Log;

import net.sharksystem.asap.android.service.ASAPService;
import net.sharksystem.asap.android.service.MacLayerEngine;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

public class LoRaEngine extends MacLayerEngine {

    private static final String CLASS_LOG_TAG = "ASAPLoRaEngine";


    private static LoRaEngine engine = null;
    private BluetoothDevice btDevice = null;

    /**
     * - Aufbau Verbindung zu BLE o. Bluetooth UART Schnittstelle
     * - Prüfen der Verbindung zum LoRa Board AT <-> AT+OK
     *
     * @param ASAPService
     * @param context
     * @return
     */
    public static LoRaEngine getASAPLoRaEngine(ASAPService ASAPService,
                                               Context context) {
        if (LoRaEngine.engine == null) {
            LoRaEngine.engine = new LoRaEngine(ASAPService, context);
        }

        return LoRaEngine.engine;
    }

    public static LoRaEngine getASAPLoRaEngine() {
        return LoRaEngine.engine;
    }

    public LoRaEngine(ASAPService asapService, Context context) {
        super(asapService, context);
    }

    private void initBluetooth() throws ASAPLoRaException {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.cancelDiscovery();

        for (BluetoothDevice btDevice : btAdapter.getBondedDevices()) {
            if (btDevice.getName().indexOf("ASAP-LoRa") == 0) { //TODO: What about more than 1 paired ASAP-LoRa Board? Or 1 avail and 1 unavail?
                this.btDevice = btDevice;
                break;
            }
        }
        if (this.btDevice == null)
            throw new ASAPLoRaException("Please pair to an ASAP-LoRa Board before Starting LoRa!");

        /**
         * uses UUID of Serial Devices for now - https://www.bluetooth.com/specifications/assigned-numbers/
         * Might be a good idea to move another uuid to define a ASAP-LoRa Node
         */
        try {
            BluetoothSocket btSocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btSocket.connect();
            btSocket.getOutputStream().write("AT".getBytes());

            while(true){ //TODO: Do not Activewait...
                if(btSocket.getInputStream().available() > 0) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(btSocket.getInputStream()));
                    StringBuilder sb = new StringBuilder(btSocket.getInputStream().available());
                    do {
                        sb.append(br.readLine()).append("\n");
                    } while(br.ready());
                    Log.i(this.CLASS_LOG_TAG, "LoRa Board said: "+sb.toString());
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        Log.i(this.CLASS_LOG_TAG, "MacLayerEngine.start() called");
        try {
            this.initBluetooth();
        } catch (ASAPLoRaException e) {
            Log.e(this.CLASS_LOG_TAG, e.getMessage());
        }
    }

    @Override
    public void stop() {
        Log.i(this.CLASS_LOG_TAG, "MacLayerEngine.stop() called");

    }

    @Override
    public boolean tryReconnect() {
        Log.i(this.CLASS_LOG_TAG, "MacLayerEngine.tryReconnect() called");
        return false;
    }

    @Override
    public void checkConnectionStatus() {
        Log.i(this.CLASS_LOG_TAG, "MacLayerEngine.checkConnectionStatus() called");

    }
}
