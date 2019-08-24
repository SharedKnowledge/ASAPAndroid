package net.sharksystem.asap.android.bluetooth;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.ASAP;

import java.io.IOException;

class BluetoothServerSocketThread extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    private final BluetoothEngine btEngine;

    public BluetoothServerSocketThread(BluetoothEngine btEngine) throws IOException, ASAPException {
        this.btEngine = btEngine;
        this. mmServerSocket = this.btEngine.getBTAdapter().listenUsingRfcommWithServiceRecord(
                    ASAP.ASAP_SERVICE_NAME, ASAP.ASAP_UUID);
    }

    private String getLogStart() {
        return "BTServerSocketThread";
    }

    private boolean stopped = false;
    private Thread acceptThread = null;

    void stopAccept() {
        this.stopped = true;
        if(this.acceptThread != null) {
            this.acceptThread.interrupt();
        }
    }

    public void run() {
        BluetoothSocket socket = null;
        this.acceptThread = Thread.currentThread();

        // Keep listening until exception occurs or a socket is returned.
        while (!this.stopped) {
            try {
                socket = mmServerSocket.accept();
                Log.d(this.getLogStart(), "no BT connection established");

                // this must be handled here - see comments in BluetoothEngine
                if(this.btEngine.shouldConnectToMACPeer(socket.getRemoteDevice().getAddress())) {
                    this.btEngine.handleBTSocket(socket);
                } else {
                    Log.d(this.getLogStart(), "shall not talk to this peer - close");
                    try {
                        socket.close();
                    }
                    catch(IOException e) {
                        Log.d(this.getLogStart(), "could not close - ignore: "
                                + e.getLocalizedMessage());
                    }
                }
            } catch (IOException e) {
                Log.d(this.getLogStart(), "Socket's accept() method failed", e);
                break;
            }
        }
    }

    // Closes the connect socket and causes the thread to finish.
    public void close() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(this.getLogStart(), "Could not close the connect socket", e);
        }
    }
}

