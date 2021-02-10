package net.sharksystem.asap.android.lora.messages;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;


/**
 * Represents an Error Message from the ASAPLoRaBTModule, has the Payload "ERROR:<message>"
 * Is usually received, if something went wrong in sending the LoRa Message.
 */

public class ErrorASAPLoRaMessage extends AbstractASAPLoRaMessage {

    String message = "";

    ErrorASAPLoRaMessage(String message){
        this.message = message;
    }

    @Override
    public String getPayload() {
        return this.message;
    }

    /**
     * Passes itself to the @{@link LoRaCommunicationManager} so the error can be handled.
     * @param loRaCommunicationManager
     */
    @Override
    public void handleMessage(LoRaCommunicationManager loRaCommunicationManager) {
        loRaCommunicationManager.handleError(this);
    }
}