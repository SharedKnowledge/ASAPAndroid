package net.sharksystem.asap.android.lora.messages;

public class ASAPLoRaMessage extends AbstractASAPLoRaMessage {
    //these need to be public to be picked up by the json automapper
    public String message = "";
    public String address = "";

    public ASAPLoRaMessage(String address, String message){
        this.address = address;
        this.message = message;
    }

    @Override
    public String toString() {
        return this.address + ": " + this.message;
    }
}
