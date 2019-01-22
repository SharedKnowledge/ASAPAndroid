package net.sharksystem.aasp.android;

import net.sharksystem.asp3.ASP3Engine;

import java.io.InputStream;
import java.io.OutputStream;

public class AASPSession extends Thread {
    private final InputStream is;
    private final OutputStream os;
    private final ASP3Engine aaspEngine;
    private final AASPSessionListener aaspSessionListener;

    AASPSession(InputStream is, OutputStream os, ASP3Engine aaspEngine,
                AASPSessionListener aaspSessionListener) {
        this.is = is;
        this.os = os;
        this.aaspEngine = aaspEngine;
        this.aaspSessionListener = aaspSessionListener;
    }
}
