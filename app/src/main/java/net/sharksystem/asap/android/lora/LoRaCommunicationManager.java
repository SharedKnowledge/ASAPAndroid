package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import net.sharksystem.asap.android.lora.messages.ASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.AbstractASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.DeviceDiscoveredASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.DiscoverASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.ErrorASAPLoRaMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
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
    private static LoRaBTInputOutputStream ioStream = null;
    private BluetoothDevice btDevice = null;

    public LoRaCommunicationManager() throws ASAPLoRaException {

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        btAdapter.enable();
        btAdapter.cancelDiscovery();

        //TODO - move to Parameter / initialize Selection-Dialog
        for (BluetoothDevice btDevice : btAdapter.getBondedDevices()) {
            if (btDevice.getName().indexOf("ASAP-LoRa") == 0) { //TODO: What about more than 1 paired ASAP-LoRa Board? Or 1 avail and 1 unavail?
                this.btDevice = btDevice;
                break;
            }
        }
        if (this.btDevice == null)
            throw new ASAPLoRaException("Please pair to an ASAP-LoRa Board before Starting LoRa!");

        /**
         * uses UUID of Serial Devices for now - https://www.bluetooth.com/specifications/assigned-numbers/
         * Might be a good idea to move another uuid to define a ASAP-LoRa Node
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
        super.run();

        try {
            //this.ioStream.getOutputStream().write(new RawASAPLoRaMessage("AT"));
            this.ioStream.getOutputStream().write(new DiscoverASAPLoRaMessage()); //TODO, do this periodically?
            //this.ioStream.getOutputStream().write(new ASAPLoRaMessage("A2FF", "Hi there!"));

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

                //Periodically flush Write Buffers
                this.ioStream.flushASAPOutputStreams();
                //LoRa is slow, we really don't need to run at full steam all the time
                try { Thread.sleep(250); } catch (InterruptedException e) {}
            }
        } catch (IOException e) {
            Log.e(this.CLASS_LOG_TAG, e.getMessage());
            //throw new ASAPLoRaException(e);
        } finally {
            this.ioStream.close(); //cleanup after ourselves
        }
    }
}
