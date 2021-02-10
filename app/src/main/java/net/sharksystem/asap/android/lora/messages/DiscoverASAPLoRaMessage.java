package net.sharksystem.asap.android.lora.messages;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;

/**
 * Represents a Discover Message to the ASAPLoRaBTModule, has the Payload "DSCVR"
 */

public class DiscoverASAPLoRaMessage extends AbstractASAPLoRaMessage {

    public DiscoverASAPLoRaMessage() throws ASAPLoRaMessageException {
        this.setAddress("FFFF");
    }
    @Override
    public String getPayload() {
        return "DSCVR";
    }

    @Override
    public String toString() {
        return "DSCVR";
    }
}
