package net.sharksystem.asap.android.lora.messages;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;

public class DeviceDiscoveredASAPLoRaMessage extends AbstractASAPLoRaMessage {
    public String address = "";

    public DeviceDiscoveredASAPLoRaMessage(String address) {
        this.address = address;
    }

    @Override
    public void handleMessage(LoRaCommunicationManager loRaCommunicationManager) {
        loRaCommunicationManager.tryConnect(this.address);
    }

    @Override
    public String toString() {
        return "DeviceDiscoveredASAPLoRaMessage: " + this.address;
    }
}
