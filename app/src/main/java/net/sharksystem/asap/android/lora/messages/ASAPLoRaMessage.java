package net.sharksystem.asap.android.lora.messages;

public class ASAPLoRaMessage extends AbstractASAPLoRaMessage {
    //these need to be public to be picked up by the json automapper
    public String address;
    public byte[] message;

    //Constructor for Jackson
    public ASAPLoRaMessage(){
        this.address = "";
        this.message = new byte[0];
    }
    public ASAPLoRaMessage(String address, byte[] message){
        this.address = address;
        this.message = message;
    }

    @Override
    public String toString() {
        return this.address + ": " + this.message;
    }
}
