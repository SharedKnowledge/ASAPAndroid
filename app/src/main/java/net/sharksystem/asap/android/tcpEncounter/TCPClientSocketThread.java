package net.sharksystem.asap.android.tcpEncounter;

import android.util.Log;

import net.sharksystem.asap.ASAPEncounterManager;
import net.sharksystem.asap.EncounterConnectionType;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.streams.StreamPairImpl;

import java.io.IOException;
import java.net.Socket;

/**
 * This class can be used to create a TCP connection with another ASAPPeer which provides a TCP server.
 * The connection is handled by the ASAPEncounterManager.
 * The class inherits from Thread and establishes the connection in the run method.
 */
public class TCPClientSocketThread extends Thread {
    private final ASAPEncounterManager encounterManager;
    private final String host;
    private final int port;
    private TCPEncounterListener listener;

    public TCPClientSocketThread(ASAPEncounterManager encounterManager, String host, int port) {
        this.encounterManager = encounterManager;
        this.host = host;
        this.port = port;
    }

    public void setListener(TCPEncounterListener listener) {
        this.listener = listener;
    }

    private void notifyListener() {
        if(this.listener != null) {
            this.listener.onEncounterSuccess();
        }
    }

    public void run() {
        try {
            Log.d(this.getLogStart(), "create tcp client socket");
            Socket socket = new Socket(host, port);
            StreamPair streamPair = StreamPairImpl.getStreamPair(socket.getInputStream(), socket.getOutputStream());
            Log.d(this.getLogStart(), "connected - going to call handleEncounter");
            encounterManager.handleEncounter(streamPair, EncounterConnectionType.AD_HOC_LAYER_2_NETWORK);
            notifyListener();
        } catch (IOException e) {
            Log.e(this.getLogStart(), "could not connect: " + e.getLocalizedMessage());
        }
    }

    private String logStartString = null;
    private String getLogStart() {
        if(this.logStartString == null) {
            this.logStartString = String.format("TCPClientSocketThread (to %s:%d)", host, port);
        }
        return this.logStartString;
    }
}
