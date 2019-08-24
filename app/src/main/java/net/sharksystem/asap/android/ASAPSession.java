package net.sharksystem.asap.android;

import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.MultiASAPEngineFS;
import net.sharksystem.util.tcp.TCPChannelMaker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ASAPSession extends Thread {
    private TCPChannelMaker channelMaker;
    private final MultiASAPEngineFS asapEngine;
    private final ASAPSessionListener asapSessionListener;
    private InputStream is;
    private OutputStream os;

    public ASAPSession(TCPChannelMaker channelMaker, MultiASAPEngineFS asapEngine,
                       ASAPSessionListener asapSessionListener) {

        this(asapEngine, asapSessionListener);
        this.channelMaker = channelMaker;
        this.is = null;
        this.os = null;
    }

    public ASAPSession(InputStream is, OutputStream os, MultiASAPEngineFS asapEngine,
                       ASAPSessionListener asapSessionListener) {
        this(asapEngine, asapSessionListener);
        this.is = is;
        this.os = os;
        this.channelMaker = null;
    }

    private ASAPSession(MultiASAPEngineFS asapEngine,ASAPSessionListener asapSessionListener) {
        this.asapEngine = asapEngine;
        this.asapSessionListener = asapSessionListener;
    }
    
    private String getLogStart() {
        return "ASAPSession";
    }

    public void run() {
        Log.d(this.getLogStart(), "session started");

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

            this.asapSessionListener.sessionStarted();
            this.asapEngine.handleConnection(this.is, this.os);
            this.asapSessionListener.asapSessionFinished();

        } catch (IOException | ASAPException e) {
            Log.d(this.getLogStart(), "while handling connection: " + e.getLocalizedMessage());
            try {
                this.os.close();
            } catch (IOException ex) {
                // won't probably work due to failure before - ignore any further exception
            }
        }
    }
}
