package net.sharksystem.asap.android.lora.messages;

import android.util.Log;

import net.sharksystem.asap.android.lora.LoRaCommunicationManager;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;

/**
 * Represents a Device Discovered Message from the ASAPLoRaBTModule, has the Payload "DVDCR:<Address 1000-FFFE>"
 */

public class DeviceDiscoveredASAPLoRaMessage extends AbstractASAPLoRaMessage {
    private static final String CLASS_LOG_TAG = "DvcDscvrdLoRaMessage";

    public DeviceDiscoveredASAPLoRaMessage(String address) throws ASAPLoRaMessageException {
        this.setAddress(address);
    }

    @Override
    public void handleMessage(LoRaCommunicationManager loRaCommunicationManager) {
        try {
            loRaCommunicationManager.tryConnect(this.getAddress());
        } catch (ASAPLoRaMessageException e) {
            Log.i(CLASS_LOG_TAG, "Could not connect to remote client as we failed to retrieve its address.");
            Log.i(CLASS_LOG_TAG, this.toString());
        }
    }

    @Override
    public String toString() {
        try {
            return "DeviceDiscoveredASAPLoRaMessage: " + this.getAddress();
        } catch (ASAPLoRaMessageException e) {
            return "DeviceDiscoveredASAPLoRaMessage: " + e.toString();
        }
    }
}
