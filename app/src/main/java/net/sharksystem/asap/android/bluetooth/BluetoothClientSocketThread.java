package net.sharksystem.asap.android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.MacLayerEngine;

import java.io.IOException;

class BluetoothClientSocketThread extends Thread {
    private BluetoothSocket btSocket;
    private final BluetoothEngine btEngine;
    private final BluetoothDevice btDevice;

    /**
     * @param btEngine
     * @param btDevice
     * @throws IOException
     * @throws ASAPException
     */
    BluetoothClientSocketThread(BluetoothEngine btEngine, BluetoothDevice btDevice) {
        this.btEngine = btEngine;
        this.btDevice = btDevice;
    }

    public void run() {
        try {
            Log.d(this.getLogStart(), "going to call createRFCOMMSocket");
            this.btSocket = btDevice.createRfcommSocketToServiceRecord(MacLayerEngine.ASAP_UUID);
            // btEngine.getBTAdapter().cancelDiscovery(); // was strongly suggested in documentation
            Log.d(this.getLogStart(), "going to call connect");
            btSocket.connect();

            Log.d(this.getLogStart(), "connected - going to call handleBTSocket");
            this.btEngine.handleBTSocket(this.btSocket);
        } catch (IOException e) {
            Log.e(this.getLogStart(), "could not connect: " + e.getLocalizedMessage());
        }
    }

    private String getLogStart() {
        return "BTClientSocketThread";
    }
}
