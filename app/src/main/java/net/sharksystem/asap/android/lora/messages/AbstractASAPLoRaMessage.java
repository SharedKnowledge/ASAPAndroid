package net.sharksystem.asap.android.lora.messages;

import net.sharksystem.asap.android.lora.ASAPLoRaException;

//TODO
//@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, property="COMMAND")
//@JsonSubTypes( {@JsonSubTypes.Type(DiscoverASAPLoRaMessage.class), @JsonSubTypes.Type(ASAPLoRaMessage.class)})
public abstract class AbstractASAPLoRaMessage {

    public String getPayload() throws ASAPLoRaException {
        throw new ASAPLoRaException("Trying to call getPayload() on non-outgoing ASAP Message. This should never happen.");
    }

    @Override
    public String toString() {
        return "AbstractASAPLoRaMessage derived class: " + this.getClass().getName();
    }
}