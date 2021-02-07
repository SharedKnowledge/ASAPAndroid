package net.sharksystem.asap.android.lora;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaException;

import java.io.IOException;

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
                LoRaEngine.getASAPLoRaEngine().stop();
            }
        }
    }
}
