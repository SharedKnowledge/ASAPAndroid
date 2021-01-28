package net.sharksystem.asap.android.lora.messages;

public class DiscoverASAPLoRaMessage extends AbstractASAPLoRaMessage {
    @Override
    public String getPayload() {
        return "DSCVR";
    }

    @Override
    public String toString() {
        return "DSCVR";
    }
}
