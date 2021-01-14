package net.sharksystem.asap.android.lora.messages;

import android.os.Build;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ASAPLoRaMessage extends AbstractASAPLoRaMessage {
    //these need to be public to be picked up by the json automapper
    public String address;
    public byte[] message;
    public String base64message;

    //Constructor for Jackson
    public ASAPLoRaMessage() {
        this.address = "";
        this.message = new byte[0];
    }

    public ASAPLoRaMessage(String address, byte[] message) {
        this.address = address;
        this.message = message;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //TODO!
            this.base64message = new String(Base64.getMimeEncoder().encode(message), StandardCharsets.UTF_8);
        }
    }

    public ASAPLoRaMessage(String address, String base64message) {
        this.address = address;
        this.base64message = base64message.trim(); //whitespaces can be ignored, according to base64 RFC2045
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //TODO!
            this.message = Base64.getMimeDecoder().decode(this.base64message.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public String getPayload() {
        return "MSSGE@" + this.address + ":" + this.base64message;
    }

    @Override
    public String toString() {
        return "ASAPLoRaMessage (" + this.address + ":" + this.base64message + "): " + new String(this.message);
    }
}
