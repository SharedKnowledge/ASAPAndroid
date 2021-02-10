package net.sharksystem.asap.android.lora;

import android.util.Log;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaException;

import java.io.IOException;

/**
 * Blockingly reads on the BluetoothSocket, until a new ASAPLoRaMessage is received or the Socket closes
 */
public class LoRaBTListenThread extends Thread {

    private static final String CLASS_LOG_TAG = "ASAPLoRaBTListenThread";
    LoRaCommunicationManager loRaCommunicationManager = null;

    public LoRaBTListenThread(LoRaCommunicationManager loRaCommunicationManager){
        this.loRaCommunicationManager = loRaCommunicationManager;
    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            //Listen for a Message from the BT Stream & pass it on
            try {
                loRaCommunicationManager.receiveASAPLoRaMessage(this.loRaCommunicationManager.getBTInputStream().readASAPLoRaMessage());
            } catch (ASAPLoRaException | IOException e) {
                //The ASAPLoRa-Module was disconnected, so we cannot continue to run the LoRaEngine
                Log.e(this.CLASS_LOG_TAG, e.getMessage());
                Log.e(this.CLASS_LOG_TAG, "Stopping ASAPLoRaEngine...");
                LoRaEngine.getASAPLoRaEngine().stop();
            }
        }
    }
}
