package net.sharksystem.asap.android.lora.messages;

public class RawASAPLoRaMessage extends AbstractASAPLoRaMessage {

    private String rawMessage = "";

    public RawASAPLoRaMessage(String msg){
        this.rawMessage = msg;
    }

    @Override
    public String toString() {
        return this.rawMessage;
    }
}
