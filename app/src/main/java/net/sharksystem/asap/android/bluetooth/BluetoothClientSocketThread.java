package net.sharksystem.asap.android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.ASAP;

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
            this.btSocket = btDevice.createRfcommSocketToServiceRecord(ASAP.ASAP_UUID);
            // btEngine.getBTAdapter().cancelDiscovery(); // was strongly suggested in documentation
            btSocket.connect();

            this.btEngine.handleBTSocket(this.btSocket);
        } catch (IOException e) {
            Log.e("BTClientSocket", "could not connect: " + e.getLocalizedMessage());
        }
    }
}
