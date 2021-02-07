package net.sharksystem.asap.android.lora.messages;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;

import android.util.Base64;

public class ASAPLoRaMessage extends AbstractASAPLoRaMessage {
    private byte[] message;
    private String base64message;

    public ASAPLoRaMessage(String address, byte[] message) throws ASAPLoRaMessageException {
        if (message.length > 174)
            throw new ASAPLoRaMessageException("Passed a message that is too long for LoRa Transport");
        this.setAddress(address);
        this.message = message;
        this.base64message = Base64.encodeToString(message, Base64.DEFAULT);
    }

    public ASAPLoRaMessage(String address, String base64message) throws ASAPLoRaMessageException {
        this.setAddress(address);
        this.base64message = base64message.trim(); //whitespaces can be ignored, according to base64 RFC2045
        this.message = Base64.decode(this.base64message, Base64.DEFAULT);
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
