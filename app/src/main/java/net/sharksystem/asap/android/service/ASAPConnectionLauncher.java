package net.sharksystem.asap.android.service;

import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.MultiASAPEngineFS;
import net.sharksystem.asap.android.Util;
import net.sharksystem.util.tcp.TCPChannelMaker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ASAPConnectionLauncher extends Thread {
    private TCPChannelMaker channelMaker;
    private final MultiASAPEngineFS asapEngine;
    private InputStream is;
    private OutputStream os;

    public ASAPConnectionLauncher(TCPChannelMaker channelMaker, MultiASAPEngineFS asapEngine) {
        this(asapEngine);
        this.channelMaker = channelMaker;
        this.is = null;
        this.os = null;
    }

    public ASAPConnectionLauncher(InputStream is, OutputStream os, MultiASAPEngineFS asapEngine) {
        this(asapEngine);
        this.is = is;
        this.os = os;
        this.channelMaker = null;
    }

    private ASAPConnectionLauncher(MultiASAPEngineFS asapEngine) {
        this.asapEngine = asapEngine;
    }
    
    private String getLogStart() {
        return Util.getLogStart(this);
    }

    public void run() {
        Log.d(this.getLogStart(), "started");

        try {
            if(this.is == null) {
                if (!this.channelMaker.running()) {
                    Log.d(this.getLogStart(), "connection maker not running - start");
                    this.channelMaker.start();
                } else {
                    // if already running must be a server channel
                    Log.d(this.getLogStart(), "connection maker running - next connection");
                    this.channelMaker.nextConnection();
                }

                Log.d(this.getLogStart(), "channel maker started, wait for connection");
                this.channelMaker.waitUntilConnectionEstablished();
                Log.d(this.getLogStart(), "connected - start handle connection");

                this.is = this.channelMaker.getInputStream();
                this.os = this.channelMaker.getOutputStream();
            }

            Log.d(this.getLogStart(), "call asapMultiEngine to handle connection");
//            TestConnectionHandler testConnectionHandler = new TestConnectionHandler(this.is, this.os);
//            testConnectionHandler.start();
            this.asapEngine.handleConnection(this.is, this.os);
        } catch (IOException | ASAPException e) {
            Log.d(this.getLogStart(), "while laucnhing asap connection: " + e.getLocalizedMessage());
            try {
                this.os.close();
            } catch (IOException ex) {
                // won't probably work due to failure before - ignore any further exception
            }
        }
    }
}
