package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaException;
import net.sharksystem.asap.android.service.ASAPService;
import net.sharksystem.asap.android.service.MacLayerEngine;


public class LoRaEngine extends MacLayerEngine {

    private static final String CLASS_LOG_TAG = "ASAPLoRaEngine";
    private static LoRaEngine engine = null;
    private LoRaCommunicationManager loRaCommunicationManager;

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

    @Override
    public void start() {
        Log.i(this.CLASS_LOG_TAG, "MacLayerEngine.start() called");
        try {
            //TODO - move btDevice selection to Parameter / initialize Selection-Dialog
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

            btAdapter.enable();
            btAdapter.cancelDiscovery();

            for (BluetoothDevice btDevice : btAdapter.getBondedDevices()) {
                if (btDevice.getName().indexOf("ASAP-LoRa") == 0) {
                    loRaCommunicationManager = new LoRaCommunicationManager(btDevice);
                    loRaCommunicationManager.start();
                    break;
                }
            }
        } catch (ASAPLoRaException e) {
            e.printStackTrace(); //TODO
        }
    }

    @Override
    public void stop() {
        Log.i(this.CLASS_LOG_TAG, "MacLayerEngine.stop() called");
        if (loRaCommunicationManager != null) {
            loRaCommunicationManager.interrupt();
        }
    }

    @Override
    public boolean tryReconnect() {
        Log.i(this.CLASS_LOG_TAG, "MacLayerEngine.tryReconnect() called");
        return true; //assume we have a good connection, as we do not have persistent connections
    }

    @Override
    public void checkConnectionStatus() {
        Log.i(this.CLASS_LOG_TAG, "MacLayerEngine.checkConnectionStatus() called");
        //NOOP, as we do not have persistent connections
    }

    void tryConnect(String macAddress) {
        if (this.shouldConnectToMACPeer(macAddress)) {
            this.launchASAPConnection(macAddress, this.loRaCommunicationManager.getASAPInputStream(macAddress), this.loRaCommunicationManager.getASAPOutputStream(macAddress));
        } else {
            Log.d(this.CLASS_LOG_TAG, "Connection to "
                    + macAddress + " not needed.");
        }
    }
}
