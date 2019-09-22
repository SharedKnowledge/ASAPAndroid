package net.sharksystem.asap.android.service;

import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class TestConnectionHandler extends Thread {
    private final InputStream is;
    private final OutputStream os;

    public TestConnectionHandler(InputStream is, OutputStream os) throws ASAPException {
        this.is = is;
        this.os = os;
    }

    public void run() {
        String message = "Hi - this is a test message";
        byte[] messageBytes = message.getBytes();
        byte[] receivedBytes = new byte[messageBytes.length];

        // send
        try {
            Log.d(this.getLogStart(), "going to write");
            this.os.write(messageBytes);
            Log.d(this.getLogStart(), "going to read");
            this.is.read(receivedBytes);
            Log.d(this.getLogStart(), "read: " + new String(receivedBytes));
        } catch (IOException e) {
            Log.d(this.getLogStart(), e.getLocalizedMessage());
        }
    }

    private String getLogStart() {
        return Util.getLogStart(this);
    }
}
