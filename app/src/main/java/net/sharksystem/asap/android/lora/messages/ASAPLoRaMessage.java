package net.sharksystem.asap.android.lora.messages;

import android.os.Build;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaException;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ASAPLoRaMessage extends AbstractASAPLoRaMessage {
    private byte[] message;
    private String base64message;

    public ASAPLoRaMessage(String address, byte[] message) throws ASAPLoRaMessageException {
        if (message.length > 174)
            throw new ASAPLoRaMessageException("Passed a message that is too long for LoRa Transport");
        this.setAddress(address);
        this.message = message;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //TODO!
            this.base64message = new String(Base64.getEncoder().encode(message), StandardCharsets.UTF_8);
        }
    }

    public ASAPLoRaMessage(String address, String base64message) throws ASAPLoRaMessageException {
        this.setAddress(address);
        this.base64message = base64message.trim(); //whitespaces can be ignored, according to base64 RFC2045
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //TODO!
            this.message = Base64.getDecoder().decode(this.base64message.getBytes(StandardCharsets.UTF_8));
        }
    }

    public byte[] getMessage() {
        return this.message;
    }

    @Override
    public void handleMessage(LoRaCommunicationManager loRaCommunicationManager) {
        try {
            loRaCommunicationManager.appendMessage(this);
        } catch (ASAPLoRaMessageException e) {
            e.printStackTrace(); //TODO, do we need to do something about this?
        }
    }

    @Override
    public String getPayload() throws ASAPLoRaMessageException {
        return "MSSGE@" + this.getAddress() + ":" + this.base64message;
    }

    @Override
    public String toString() {
        try {
            return "ASAPLoRaMessage (" + this.getAddress() + ":" + this.base64message + "): " + new String(this.message);
        } catch (ASAPLoRaMessageException e) {
            return "ASAPLoRaMessage: " + e.toString();
        }
    }
}
