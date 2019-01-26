package net.sharksystem.util;

import android.util.Log;

import net.sharksystem.util.tcp.TCPChannelMaker;
import net.sharksystem.asp3.ASP3Engine;
import net.sharksystem.asp3.ASP3ReceivedChunkListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AASPSession extends Thread {
    private final TCPChannelMaker channelMaker;
    private final ASP3Engine aaspEngine;
    private final AASPSessionListener aaspSessionListener;
    private final ASP3ReceivedChunkListener chunkReceivedListener;

    public AASPSession(TCPChannelMaker channelMaker, ASP3Engine aaspEngine,
                       AASPSessionListener aaspSessionListener,
                       ASP3ReceivedChunkListener chunkReceivedListener) {
        this.channelMaker = channelMaker;
        this.aaspEngine = aaspEngine;
        this.aaspSessionListener = aaspSessionListener;
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

            this.aaspSessionListener.aaspSessionEnded();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
