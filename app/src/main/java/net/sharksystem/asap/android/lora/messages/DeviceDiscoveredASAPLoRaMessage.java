package net.sharksystem.asap.android.lora.messages;

public class DeviceDiscoveredASAPLoRaMessage extends AbstractASAPLoRaMessage {
    public String address = "";

    public DeviceDiscoveredASAPLoRaMessage(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "DeviceDiscoveredASAPLoRaMessage: " + this.address;
    }
}
