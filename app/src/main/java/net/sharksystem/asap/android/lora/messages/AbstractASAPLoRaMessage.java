package net.sharksystem.asap.android.lora.messages;

import android.util.Log;

import net.sharksystem.asap.android.lora.ASAPLoRaException;

public abstract class AbstractASAPLoRaMessage {
    private static final String CLASS_LOG_TAG = "AbstractASAPLoRaMessage";

    public String getPayload() throws ASAPLoRaException {
        throw new ASAPLoRaException("Trying to call getPayload() on non-outgoing ASAP Message. This should never happen.");
    }

    public static AbstractASAPLoRaMessage createASAPLoRaMessage(String rawMessage) throws ASAPLoRaException {
        Log.i(CLASS_LOG_TAG, "Raw LoRa Message Received!");
        Log.i(CLASS_LOG_TAG, "Creating AbstractASAPLoRaMessage from String: " + rawMessage);
        // rawMessage is of format: <COMMAND (5 Char)>:<Payload>
        String messageType = rawMessage.substring(0, 5);
        String messagePayload = rawMessage.substring(6);
        switch (messageType) {
            case "DSCVR":
                return new DiscoverASAPLoRaMessage(); //I don't think this can happen...?
            case "DVDCR":
                return new DeviceDiscoveredASAPLoRaMessage(messagePayload);
            case "MSSGE":
                String messageAddress = messagePayload.substring(0, 4);
                String message = messagePayload.substring(5);
                return new ASAPLoRaMessage(messageAddress, message);
        }
        throw new ASAPLoRaException("Recieved invalid Message Type: " + rawMessage);
    }

    @Override
    public String toString() {
        return "AbstractASAPLoRaMessage derived class: " + this.getClass().getName();
    }
}