package net.sharksystem.asap.android.lora.messages;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;

/**
 * Interface for Messages from or to the ASAPLoRaBTModule / the LoRaEngine.
 * Represents the communication protocol in Java.
 */
public interface ASAPLoRaMessageInterface {
    String getPayload() throws ASAPLoRaMessageException;
    String getAddress() throws ASAPLoRaMessageException;
    void setAddress(String address) throws ASAPLoRaMessageException;
    void handleMessage(LoRaCommunicationManager loRaCommunicationManager) throws ASAPLoRaMessageException;
}
