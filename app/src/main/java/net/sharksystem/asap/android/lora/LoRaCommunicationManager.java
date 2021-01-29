package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaException;
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

    @Override
    public void run() {
        try {
            //this.ioStream.getOutputStream().write(new RawASAPLoRaMessage("AT"));
            this.ioStream.getOutputStream().write(new DiscoverASAPLoRaMessage()); //TODO, do this periodically?
            //this.ioStream.getOutputStream().write(new ASAPLoRaMessage("A2FF", "Hi there!"));
            long lastBufferFlush = System.currentTimeMillis();
            while (!this.isInterrupted()) {
                if (this.ioStream.getInputStream().available() > 0) {
                    AbstractASAPLoRaMessage asapLoRaMessage = this.ioStream.getInputStream().readASAPLoRaMessage();
                    Log.i(this.CLASS_LOG_TAG, "Message recieved: " + asapLoRaMessage.toString());
                    //TODO, this is smelly... visitorpattern? handleMessage() in abstract?
                    if (asapLoRaMessage instanceof ASAPLoRaMessage) {
                        //New Message inbound, write to corresponding stream of ASAPPeer
                        this.ioStream.getASAPInputStream(((ASAPLoRaMessage) asapLoRaMessage).address).appendData(((ASAPLoRaMessage) asapLoRaMessage).message);
                    } else if (asapLoRaMessage instanceof DeviceDiscoveredASAPLoRaMessage) {
                        //New Device in Range found
                        LoRaEngine.getASAPLoRaEngine().tryConnect(((DeviceDiscoveredASAPLoRaMessage) asapLoRaMessage).address);
                    } else if (asapLoRaMessage instanceof ErrorASAPLoRaMessage) {
                        //LoRa Error occured
                        // TODO
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
            //throw new ASAPLoRaException(e);
        } finally {
            this.ioStream.close(); //cleanup after ourselves
            Log.i(CLASS_LOG_TAG, "Streams were closed. Shutting down.");
        }
    }
}
