package net.sharksystem.asap.android.lora.messages;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;

public class DeviceDiscoveredASAPLoRaMessage extends AbstractASAPLoRaMessage {

    public DeviceDiscoveredASAPLoRaMessage(String address) throws ASAPLoRaMessageException {
        this.setAddress(address);
    }

    @Override
    public void handleMessage(LoRaCommunicationManager loRaCommunicationManager) {
        try {
            loRaCommunicationManager.tryConnect(this.getAddress());
        } catch (ASAPLoRaMessageException e) {
            e.printStackTrace(); //TODO do we need to do something about this?
        }
    }

    @Override
    public String toString() {
        try {
            return "DeviceDiscoveredASAPLoRaMessage: " + this.getAddress();
        } catch (ASAPLoRaMessageException e) {
            return "DeviceDiscoveredASAPLoRaMessage: " + e.toString();
        }
    }
}
