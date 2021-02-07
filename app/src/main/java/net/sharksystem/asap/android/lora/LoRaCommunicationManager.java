package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaException;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;
import net.sharksystem.asap.android.lora.messages.ASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.AbstractASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.DiscoverASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.ErrorASAPLoRaMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.UUID;

public class LoRaCommunicationManager extends Thread {
    private static final String CLASS_LOG_TAG = "ASAPLoRaCommManager";
    private static final long FLUSH_BUFFER_TIMEOUT = 250;
    private static final long DISCOVER_MESSAGE_TIMEOUT = 60 * 1000; //60s in ms
    private static final long CONNECTION_ACTIVE_TIMEOUT = DISCOVER_MESSAGE_TIMEOUT * 10; //60s in ms
    private static LoRaBTInputOutputStream ioStream = null;
    private BluetoothDevice btDevice;
    private LoRaBTListenThread loRaBTListenThread = null;
    private HashMap<String, Long> lastMessageTimeLog = new HashMap<>();

    public LoRaCommunicationManager(BluetoothDevice bluetoothDevice) throws ASAPLoRaException {

        this.btDevice = bluetoothDevice;

        if (this.btDevice == null)
            throw new ASAPLoRaException("Please pair to an ASAP-LoRa Board before Starting LoRa!");

        /**
         * uses UUID of Serial Devices for now - https://www.bluetooth.com/specifications/assigned-numbers/
         * Might be a good idea to move another uuid to define a ASAP-LoRa Node in the future
         */
        try {
            BluetoothSocket btSocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btSocket.connect();
            this.ioStream = new LoRaBTInputOutputStream(btSocket);
        } catch (IOException e) {
            //Cleanup...
            if (this.loRaBTListenThread != null)
                this.loRaBTListenThread.interrupt();
            if (this.ioStream != null)
                this.ioStream.close();
            //...then bubble up the exception
            throw new ASAPLoRaException(e);
        }
    }

    public OutputStream getASAPOutputStream(String mac) {
        return this.ioStream.getASAPOutputStream(mac);
    }

    public InputStream getASAPInputStream(String mac) {
        return this.ioStream.getASAPInputStream(mac);
    }

    public LoRaBTInputOutputStream.LoRaBTInputStream getBTInputStream() {
        return this.ioStream.getInputStream();
    }

    public void tryConnect(String address) {
        LoRaEngine.getASAPLoRaEngine().tryConnect(address);
    }

    public void receiveASAPLoRaMessage(AbstractASAPLoRaMessage abstractASAPLoRaMessage) throws ASAPLoRaException {
        Log.i(this.CLASS_LOG_TAG, "Message received: " + abstractASAPLoRaMessage.toString());
        abstractASAPLoRaMessage.handleMessage(this);

        this.lastMessageTimeLog.put(abstractASAPLoRaMessage.getAddress(), System.currentTimeMillis());
    }

    public void appendMessage(ASAPLoRaMessage asapLoRaMessage) throws ASAPLoRaMessageException {
        this.ioStream.getASAPInputStream(asapLoRaMessage.getAddress()).appendData(asapLoRaMessage.getMessage());
    }

    public void handleError(ErrorASAPLoRaMessage asapLoRaMessage) {
        Log.e(CLASS_LOG_TAG, "ErrorASAPLoRaMessage discovered: " + asapLoRaMessage.getPayload());
    }

    @Override
    public void run() {
        try {
            //Start Listening for new Messages
            this.loRaBTListenThread = new LoRaBTListenThread(this);
            this.loRaBTListenThread.start();
            long lastBufferFlush = System.currentTimeMillis();
            long lastDiscoverMessage = 0;

            while (!this.isInterrupted()) {
                //Periodically send Discover Message
                if ((System.currentTimeMillis() - lastDiscoverMessage) > this.DISCOVER_MESSAGE_TIMEOUT) {
                    this.ioStream.getOutputStream().write(new DiscoverASAPLoRaMessage());
                    lastDiscoverMessage = System.currentTimeMillis();

                    //Periodically check last message times and close streams
                    for (HashMap.Entry<String, Long> lastMessageTime :
                            this.lastMessageTimeLog.entrySet()) {
                        if ((System.currentTimeMillis() - lastMessageTime.getValue()) > (CONNECTION_ACTIVE_TIMEOUT))
                            ioStream.closeASAPStream(lastMessageTime.getKey());

                    }
                }

                //Periodically flush Buffers
                if ((System.currentTimeMillis() - lastBufferFlush) > this.FLUSH_BUFFER_TIMEOUT) {
                    this.ioStream.flushASAPOutputStreams();
                    lastBufferFlush = System.currentTimeMillis();
                }
            }

            Log.i(CLASS_LOG_TAG, "Thread was interrupted, starting Shutdown.");
        } catch (IOException | ASAPLoRaException e) {
            Log.e(this.CLASS_LOG_TAG, e.getMessage());
        } finally {
            //cleanup after ourselves
            this.loRaBTListenThread.interrupt(); //Interrupt our Listen Thread
            this.ioStream.close(); //Close the Streams
            Log.i(CLASS_LOG_TAG, "Streams were closed. Shutting down.");
        }
    }
}
