package net.sharksystem.util;

import net.sharksystem.util.tcp.TCPChannelMaker;
import net.sharksystem.asp3.ASP3Engine;
import net.sharksystem.asp3.ASP3ReceivedChunkListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AASPSession extends Thread {
    private final TCPChannelMaker channelMaker;
    private InputStream is;
    private OutputStream os;
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
                this.channelMaker.start();
            }

            System.out.println("AASPSession: channel maker started, wait for connection");
            this.channelMaker.waitUntilConnectionEstablished();
            System.out.println("AASPSession: connected - start handle connection");
            this.aaspEngine.handleConnection(this.is, this.os, this.chunkReceivedListener);
            this.aaspSessionListener.aaspSessionEnded();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
