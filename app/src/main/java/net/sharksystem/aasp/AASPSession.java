package net.sharksystem.aasp;

import net.sharksystem.asp3.ASP3Engine;
import net.sharksystem.asp3.ASP3ReceivedChunkListener;

import java.io.InputStream;
import java.io.OutputStream;

public class AASPSession extends Thread {
    private final InputStream is;
    private final OutputStream os;
    private final ASP3Engine aaspEngine;
    private final AASPSessionListener aaspSessionListener;
    private final ASP3ReceivedChunkListener chunkReceivedListener;

    AASPSession(InputStream is, OutputStream os, ASP3Engine aaspEngine,
                AASPSessionListener aaspSessionListener,
                ASP3ReceivedChunkListener chunkReceivedListener) {
        this.is = is;
        this.os = os;
        this.aaspEngine = aaspEngine;
        this.aaspSessionListener = aaspSessionListener;
        this.chunkReceivedListener = chunkReceivedListener;
    }

    public void run() {
        this.aaspEngine.handleConnection(this.is, this.os, this.chunkReceivedListener);

        this.aaspSessionListener.aaspSessionEnded();
    }
}
