package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaException;
import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;
import net.sharksystem.asap.android.lora.messages.ASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.AbstractASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.DeviceDiscoveredASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.DiscoverASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.ErrorASAPLoRaMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class LoRaCommunicationManager extends Thread {
    /**
     * Diese Klasse bildet 3 Zwecke ab:
     * - die Kommunikation mit dem SX1278 via {@link LoRaBTInputOutputStream}
     * - Ersatz der "isConnected" Logiken aus WiFi und BT (vgl SYN/ACK?)
     * --> Damit Verwaltung der versch. Input/OutputStreams pro discovertem Peer
     * - Discovery neuer Peers und Benachrichtigung der @{@link LoRaEngine}
     */
    private static final String CLASS_LOG_TAG = "ASAPLoRaCommManager";
    private static final long FLUSH_BUFFER_TIMEOUT = 250;
    private static LoRaBTInputOutputStream ioStream = null;
    private BluetoothDevice btDevice = null;
    private LoRaBTListenThread loRaBTListenThread = null;

    public LoRaCommunicationManager(BluetoothDevice bluetoothDevice) throws ASAPLoRaException {

        this.btDevice = bluetoothDevice;

        if (this.btDevice == null)
            throw new ASAPLoRaException("Please pair to an ASAP-LoRa Board before Starting LoRa!");

        /**
         * uses UUID of Serial Devices for now - https://www.bluetooth.com/specifications/assigned-numbers/
         * Might be a good idea to move another uuid to define a ASAP-LoRa Node? TODO
         */
        try {
            BluetoothSocket btSocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btSocket.connect();
            this.ioStream = new LoRaBTInputOutputStream(btSocket);
        } catch (IOException e) {
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
    }

    public void appendMessage(ASAPLoRaMessage asapLoRaMessage) {
        this.ioStream.getASAPInputStream(asapLoRaMessage.address).appendData(asapLoRaMessage.message);
    }

    @Override
    public void run() {
        try {
            //Start Listening for new Messages
            this.loRaBTListenThread = new LoRaBTListenThread(this);
            this.loRaBTListenThread.start();
            //Announce our presence
            this.ioStream.getOutputStream().write(new DiscoverASAPLoRaMessage()); //TODO, do this periodically?
            long lastBufferFlush = System.currentTimeMillis();
            while (!this.isInterrupted()) {
                //Periodically flush Buffers
                if ((System.currentTimeMillis() - lastBufferFlush) > this.FLUSH_BUFFER_TIMEOUT) {
                    this.ioStream.flushASAPOutputStreams();
                    lastBufferFlush = System.currentTimeMillis();
                }
            }
            Log.i(CLASS_LOG_TAG, "Thread was interrupted, starting Shutdown.");
        } catch (IOException | ASAPLoRaException e) {
            Log.e(this.CLASS_LOG_TAG, e.getMessage());
            //throw new ASAPLoRaException(e);
        } finally {
            //cleanup after ourselves
            this.loRaBTListenThread.interrupt(); //Interrupt our Listen Thread
            this.ioStream.close(); //Close the Streams
            Log.i(CLASS_LOG_TAG, "Streams were closed. Shutting down.");
        }
    }
}
