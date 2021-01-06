package net.sharksystem.asap.android.lora.messages;

//TODO
//@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, property="COMMAND")
//@JsonSubTypes( {@JsonSubTypes.Type(DiscoverASAPLoRaMessage.class), @JsonSubTypes.Type(ASAPLoRaMessage.class)})
public abstract class AbstractASAPLoRaMessage {
    @Override
    public String toString() {
        return "AbstractASAPLoRaMessage derived class: " + this.getClass().getName();
    }
}