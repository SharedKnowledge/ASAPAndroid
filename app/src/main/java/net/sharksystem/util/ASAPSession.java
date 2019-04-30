package net.sharksystem.util;

import android.util.Log;

import net.sharksystem.asap.ASAPEngine;
import net.sharksystem.asap.ASAPReceivedChunkListener;
import net.sharksystem.util.tcp.TCPChannelMaker;

import java.io.IOException;

public class ASAPSession extends Thread {
    private final TCPChannelMaker channelMaker;
    private final ASAPEngine aaspEngine;
    private final ASAPSessionListener ASAPSessionListener;
    private final ASAPReceivedChunkListener chunkReceivedListener;

    public ASAPSession(TCPChannelMaker channelMaker, ASAPEngine aaspEngine,
                       ASAPSessionListener ASAPSessionListener,
                       ASAPReceivedChunkListener chunkReceivedListener) {
        this.channelMaker = channelMaker;
        this.aaspEngine = aaspEngine;
        this.ASAPSessionListener = ASAPSessionListener;
        this.chunkReceivedListener = chunkReceivedListener;
    }

    public void run() {
        System.out.println("AASPSession: start");
        try {
            if(!this.channelMaker.running()) {
                Log.d("AASPSession:", "connection maker not running - start");
                this.channelMaker.start();
            } else {
                // if already running must be a server channel
                Log.d("AASPSession:", "connection maker running - next connection");
                this.channelMaker.nextConnection();
            }

            Log.d("AASPSession:", "channel maker started, wait for connection");
            this.channelMaker.waitUntilConnectionEstablished();
            Log.d("AASPSession:", "connected - start handle connection");

            this.aaspEngine.handleConnection(
                    this.channelMaker.getInputStream(),
                    this.channelMaker.getOutputStream(),
                    this.chunkReceivedListener);

            this.ASAPSessionListener.aaspSessionEnded();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
