package net.sharksystem.asap.android.lora;

import android.content.Context;
import android.util.Log;

import net.sharksystem.asap.android.service.ASAPService;
import net.sharksystem.asap.android.service.MacLayerEngine;


public class LoRaEngine extends MacLayerEngine {

    private static final String CLASS_LOG_TAG = "ASAPLoRaEngine";
    private static LoRaEngine engine = null;
    private LoRaCommunicationManager loRaCommunicationManager;

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

    @Override
    public void start() {
        Log.i(this.CLASS_LOG_TAG, "MacLayerEngine.start() called");
        try {
            loRaCommunicationManager = new LoRaCommunicationManager();
            loRaCommunicationManager.start();
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
        return false;
    }

    @Override
    public void checkConnectionStatus() {
        Log.i(this.CLASS_LOG_TAG, "MacLayerEngine.checkConnectionStatus() called");

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
