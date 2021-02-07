package net.sharksystem.asap.android.lora.messages;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;

public class ErrorASAPLoRaMessage extends AbstractASAPLoRaMessage {

    String message = "";

    ErrorASAPLoRaMessage(String message){
        this.message = message;
    }

    @Override
    public String getPayload() {
        return this.message;
    }

    @Override
    public void handleMessage(LoRaCommunicationManager loRaCommunicationManager) {
        loRaCommunicationManager.handleError(this);
    }
}