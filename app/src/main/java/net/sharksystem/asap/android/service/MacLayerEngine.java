package net.sharksystem.asap.android.service;

import android.content.Context;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.MultiASAPEngineFS;
import net.sharksystem.asap.protocol.ASAPConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class MacLayerEngine {
    // TODO - create a real UUID.
    public static final UUID ASAP_UUID = UUID.fromString("42ba5e3b-d4f0-4578-91dc-8dab6c2067ae");
    public static final String ASAP_SERVICE_NAME = "ASAP";

    private final ASAPService asapService;
    private final Context context;

    public static final long DEFAULT_WAIT_BEFORE_RECONNECT_TIME = 1000*60; // a minute
    private final long waitBeforeReconnect;

    public MacLayerEngine(ASAPService asapService, Context context) {
        this(asapService, context, DEFAULT_WAIT_BEFORE_RECONNECT_TIME);
    }

    public MacLayerEngine(ASAPService asapService, Context context, long waitingPeriod) {
        this.asapService = asapService;
        this.context = context;
        this.waitBeforeReconnect = waitingPeriod;
    }

    protected Context getContext() {
        return this.context;
    }

    protected ASAPService getAsapService() {
        return this.asapService;
    }

    public abstract void start();
    public abstract void stop();

    public void restart() {
        this.stop();
        this.start();
    }

    /** keeps info about device we have tried (!!) recently to connect
     * <MAC address, connection time>
     */
    private Map<String, Date> encounteredDevices = new HashMap<>();

    /**
     * Method is to be called when a new peer is found on a max layer.
     * That MAC address is kept.
     *
     * @param macAddress
     * @return true if a connection should be establised - either peer is unknown or
     * waiting period is over
     */
    public boolean shouldConnectToMACPeer(String macAddress) {
        Date now = new Date();
        Date lastEncounter = this.encounteredDevices.get(macAddress);

        if(lastEncounter == null) {
            // never met this peer - keep it and say yes
            Log.d(this.getLogStart(), "device not in encounteredDevices - should connect");
            this.encounteredDevices.put(macAddress, now);
            return true;
        }

        // calculate reconnection time

        // get current time, in its incarnation as date
        long nowInMillis = System.currentTimeMillis();
        long reconnectedBeforeInMillis = nowInMillis - this.waitBeforeReconnect;
        Date reconnectBefore = new Date(reconnectedBeforeInMillis);

        Log.d(this.getLogStart(),"now: " + now.toString());
        Log.d(this.getLogStart(),"connectBefore: " + reconnectBefore.toString());

        // known peer
        Log.d(this.getLogStart(), "device (" + macAddress + ") in encounteredDevices list?");
        // it was in the list
        if(lastEncounter.before(reconnectBefore)) {
            Log.d(this.getLogStart(), "yes - should connect: " + macAddress);
            // remember that
            this.encounteredDevices.put(macAddress, now);
            return true;
        }

        Log.d(this.getLogStart(), "should not connect - recently met: " + macAddress);
        return false;
    }

    private String getLogStart() {
        return "ASAPMacLayerEngine";
    }

    protected void launchASAPConnection(
            String address, InputStream inputStream, OutputStream outputStream) {
/*
        // set up new ASAP Session on that connection
        ASAPConnectionLauncher asapConnectionLauncher =
                new ASAPConnectionLauncher(
                        inputStream, outputStream, this.getAsapService().getASAPEngine());

        asapConnectionLauncher.start();
*/
        Log.d(this.getLogStart(), "going to launch a new asap connection");

        try {
            Log.d(this.getLogStart(), "call asapMultiEngine to handle connection");
//            TestConnectionHandler testConnectionHandler = new TestConnectionHandler(this.is, this.os);
//            testConnectionHandler.start();
            ASAPConnection asapConnection =
                    this.getAsapService().getASAPEngine().handleConnection(inputStream, outputStream);
        } catch (IOException | ASAPException e) {
            Log.d(this.getLogStart(), "while lauching asap connection: " + e.getLocalizedMessage());
        }
    }
}
