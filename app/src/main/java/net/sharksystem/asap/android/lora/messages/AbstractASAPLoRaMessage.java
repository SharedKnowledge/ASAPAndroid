package net.sharksystem.asap.android.lora.messages;

import android.util.Log;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;

/**
 * abstract implementation of {@link ASAPLoRaMessageInterface},
 * adding some errorhandling and a ASAPLoRaMessage-Factory
 */
public abstract class AbstractASAPLoRaMessage implements ASAPLoRaMessageInterface {
    private static final String CLASS_LOG_TAG = "AbstractASAPLoRaMessage";
    private String address = null; //Address this message is for / from, depending on context

    public String getPayload() throws ASAPLoRaMessageException {
        throw new ASAPLoRaMessageException("Trying to call getPayload() on non-outgoing ASAP Message. This should never happen.");
    }

    public String getAddress() throws ASAPLoRaMessageException {
        if(this.address == null)
            throw new ASAPLoRaMessageException("Trying to call getAddress() on ASAP Message without address.");
        return this.address;
    }

    public void setAddress(String address) throws ASAPLoRaMessageException {
        if(address == null || address.equals("") || address.length() != 4)
            throw new ASAPLoRaMessageException("Trying to call setAddress() with empty or invalid address.");
        this.address = address;
    }

    /**
     * NOOP-Implementation of the Handler-Message for received ASAPLoRaMessages
     * @param loRaCommunicationManager
     * @throws ASAPLoRaMessageException
     */
    public void handleMessage(LoRaCommunicationManager loRaCommunicationManager) throws ASAPLoRaMessageException {
        throw new ASAPLoRaMessageException("Tried handling a message, that cannot be handled.");
    }

    /**
     * Abstract Factory for Messages, creates @{@link ASAPLoRaMessageInterface}-Instances from raw
     * messages received by the ASAPLoRaBTModule.
     *
     * @param rawMessage
     * @return
     * @throws ASAPLoRaMessageException
     */
    public static ASAPLoRaMessageInterface createASAPLoRaMessage(String rawMessage) throws ASAPLoRaMessageException {
        Log.i(CLASS_LOG_TAG, "Raw LoRa Message Received!");
        Log.i(CLASS_LOG_TAG, "Creating AbstractASAPLoRaMessage from String: " + rawMessage);
        // rawMessage is of format: <COMMAND (5 Char)>:<Payload> or <COMMAND (5 Char)>@<Address (4 Char)>:<Payload>
        if (rawMessage.equals("") || rawMessage.length() < 6)
            throw new ASAPLoRaMessageException("Invalid Message Format: " + rawMessage);
        if (!(
                rawMessage.substring(5, 6).equals(":") ||
                        (rawMessage.substring(5, 6).equals("@") && rawMessage.substring(10, 11).equals(":"))
        ))
            throw new ASAPLoRaMessageException("Invalid Message Format: " + rawMessage);
        String messageType = rawMessage.substring(0, 5);
        String messagePayload = rawMessage.substring(6);
        switch (messageType) {
            case "DVDCR":
                return new DeviceDiscoveredASAPLoRaMessage(messagePayload);
            case "MSSGE":
                String messageAddress = messagePayload.substring(0, 4);
                String message = messagePayload.substring(5);
                return new ASAPLoRaMessage(messageAddress, message);
            case "ERROR":
                return new ErrorASAPLoRaMessage(messagePayload);
        }
        throw new ASAPLoRaMessageException("Recieved invalid Message Type: " + rawMessage);
    }
}