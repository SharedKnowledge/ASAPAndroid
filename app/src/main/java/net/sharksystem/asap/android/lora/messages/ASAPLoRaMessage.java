package net.sharksystem.asap.android.lora.messages;

import android.util.Base64;
import android.util.Log;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;

import java.io.IOException;

/**
 * Represents a Message from or to the ASAPLoRaBTModule, has the Payload "MSSGE:<Address 1000-FFFE>:<payload>"
 *
 * As LoRa-Packages cannot send more than 250 byte of data in one Message, the limit has to be less
 * than 175 characters, since the payload is base64-encoded and the max length of a base64 message
 * is calculated by 4 * ceil(<characters> / 3)
 */

public class ASAPLoRaMessage extends AbstractASAPLoRaMessage {
    private static final String CLASS_LOG_TAG = "ASAPLoRaMessage";
    private byte[] message;
    private String base64message;

    /**
     * Creates a message for passing to the ASAPLoRaBTModule from the byte[]-data and the address,
     * then encoding the data to base64.
     *
     * @param address
     * @param message
     * @throws ASAPLoRaMessageException
     */
    public ASAPLoRaMessage(String address, byte[] message) throws ASAPLoRaMessageException {
        if (message.length > 174)
            throw new ASAPLoRaMessageException("Passed a message that is too long for LoRa Transport");
        this.setAddress(address);
        this.message = message;
        //NO_WRAP to stop the Base64 Util to terminate the string with a newline, as we add our own LF
        this.base64message = Base64.encodeToString(message, Base64.NO_WRAP);
    }

    /**
     * Creates a message for handling in the {@link LoRaCommunicationManager} from the base64-encoded
     * data String from the ASAPLoRaBTModule, decoding the base64 data to byte[].
     * @param address
     * @param base64message
     * @throws ASAPLoRaMessageException
     */
    public ASAPLoRaMessage(String address, String base64message) throws ASAPLoRaMessageException {
        this.setAddress(address);
        this.base64message = base64message.trim(); //whitespaces can be ignored, according to RFC2045
        this.message = Base64.decode(this.base64message, Base64.DEFAULT);
    }

    public byte[] getMessage() {
        return this.message;
    }

    /**
     * Gets called from the {@link LoRaCommunicationManager} if this message should be handled.
     * Passes itself to the corresponding LoRaASAPInputStream
     *
     * @param loRaCommunicationManager
     */
    @Override
    public void handleMessage(LoRaCommunicationManager loRaCommunicationManager) {
        try {
            loRaCommunicationManager.appendMessage(this);
        } catch (ASAPLoRaMessageException e) {
            //In case we're not able to communicate with this peer, try to close the connection
            Log.e(CLASS_LOG_TAG, e.getMessage());
            try {
                loRaCommunicationManager.getASAPInputStream(this.getAddress()).close();
            } catch (ASAPLoRaMessageException | IOException closingException) {
                /**
                 * In case we were not able to close this stream, something is seriously wrong.
                 * Halt all communication over LoRa.
                 */
                Log.e(CLASS_LOG_TAG, closingException.getMessage());
                loRaCommunicationManager.interrupt();
            }
        }
    }

    /**
     * Returns the payload for sending this message to the ASAPLoRaBTModule through the BluetootSocket
     *
     * @return
     * @throws ASAPLoRaMessageException
     */
    @Override
    public String getPayload() throws ASAPLoRaMessageException {
        return "MSSGE:" + this.getAddress() + ":" + this.base64message;
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
