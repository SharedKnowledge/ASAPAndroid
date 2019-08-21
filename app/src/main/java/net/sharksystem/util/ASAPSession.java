package net.sharksystem.util;

import android.util.Log;

import net.sharksystem.asap.ASAPEngine;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPReceivedChunkListener;
import net.sharksystem.asap.MultiASAPEngineFS;
import net.sharksystem.util.tcp.TCPChannelMaker;

import java.io.IOException;

public class ASAPSession extends Thread {
    private final TCPChannelMaker channelMaker;
    private final MultiASAPEngineFS asapEngine;
    private final ASAPSessionListener ASAPSessionListener;
    private final ASAPReceivedChunkListener chunkReceivedListener;

    public ASAPSession(TCPChannelMaker channelMaker, MultiASAPEngineFS asapEngine,
                       ASAPSessionListener ASAPSessionListener,
                       ASAPReceivedChunkListener chunkReceivedListener) {
        this.channelMaker = channelMaker;
        this.asapEngine = asapEngine;
        this.ASAPSessionListener = ASAPSessionListener;
        this.chunkReceivedListener = chunkReceivedListener;
    }

    public void run() {
        System.out.println("ASAPSession: start");
        try {
            if(!this.channelMaker.running()) {
                Log.d("ASAPSession:", "connection maker not running - start");
                this.channelMaker.start();
            } else {
                // if already running must be a server channel
                Log.d("ASAPSession:", "connection maker running - next connection");
                this.channelMaker.nextConnection();
            }

            Log.d("ASAPSession:", "channel maker started, wait for connection");
            this.channelMaker.waitUntilConnectionEstablished();
            Log.d("ASAPSession:", "connected - start handle connection");

            this.asapEngine.handleConnection(
                    this.channelMaker.getInputStream(),
                    this.channelMaker.getOutputStream());

            this.ASAPSessionListener.aaspSessionEnded();
        } catch (IOException | ASAPException e) {
            e.printStackTrace();
            Log.d("ASAPSession:", "while handling connection");
        }
    }
}
