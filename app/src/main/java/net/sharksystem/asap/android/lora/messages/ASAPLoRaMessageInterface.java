package net.sharksystem.asap.android.lora.messages;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;

public interface ASAPLoRaMessageInterface {
    String getPayload() throws ASAPLoRaMessageException;
    String getAddress() throws ASAPLoRaMessageException;
    void setAddress(String address) throws ASAPLoRaMessageException;
    void handleMessage(LoRaCommunicationManager loRaCommunicationManager) throws ASAPLoRaMessageException;
}
