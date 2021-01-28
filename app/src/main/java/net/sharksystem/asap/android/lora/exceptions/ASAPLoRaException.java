package net.sharksystem.asap.android.lora.exceptions;

import net.sharksystem.asap.ASAPException;

public class ASAPLoRaException extends ASAPException {

    public ASAPLoRaException(String message) {
        super(message);
    }

    public ASAPLoRaException(Exception e){
        super(e.getMessage(), e.getCause());
    }
}
