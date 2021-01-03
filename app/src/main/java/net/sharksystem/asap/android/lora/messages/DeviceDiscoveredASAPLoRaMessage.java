package net.sharksystem.asap.android.lora.messages;

public class DeviceDiscoveredASAPLoRaMessage extends AbstractASAPLoRaMessage {
    //needs to be public to be picked up by the json automapper
    public String address = "";

    public DeviceDiscoveredASAPLoRaMessage(String address) {
        this.address = address;
    }

    //Constructor for Jackson
    public DeviceDiscoveredASAPLoRaMessage() {
        this.address = "";
    }

    @Override
    public String toString() {
        return "DeviceDiscoveredASAPLoRaMessage: " + this.address;
    }
}
