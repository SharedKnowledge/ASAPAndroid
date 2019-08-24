package net.sharksystem.asap.android;

import android.content.Context;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class MacLayerEngine implements ASAPSessionListener {
    // TODO - create a real UUID.
    public static final UUID ASAP_UUID = UUID.fromString("42ba5e3b-d4f0-4578-91dc-8dab6c2067ae");
    public static final String ASAP_SERVICE_NAME = "ASAP";

    private final ASAPService ASAPService;
    private final Context context;

    public static final long DEFAULT_WAIT_BEFORE_RECONNECT_TIME = 1000*60; // a minute
    private final long waitBeforeReconnect;

    public MacLayerEngine(ASAPService asapService, Context context) {
        this(asapService, context, DEFAULT_WAIT_BEFORE_RECONNECT_TIME);
    }

    public MacLayerEngine(ASAPService asapService, Context context, long waitingPeriod) {
        this.ASAPService = asapService;
        this.context = context;
        this.waitBeforeReconnect = waitingPeriod;
    }

    protected Context getContext() {
        return this.context;
    }

    protected ASAPService getASAPService() {
        return this.ASAPService;
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
        Log.d(this.getLogStart(), "device in encounteredDevices list");
        // it was in the list
        if(lastEncounter.before(reconnectBefore)) {
            Log.d(this.getLogStart(), "yes - should connect - waiting period is over");
            // remember that
            this.encounteredDevices.put(macAddress, now);
            return true;
        }

        Log.d(this.getLogStart(), "no - should not connect - still in waiting period");
        return false;
    }

    private String getLogStart() {
        return "ASAPMacLayerEngine";
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //                        AASPSessionListener interface support                     //
    //////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void sessionStarted() {
        Log.d(this.getLogStart(), "ASAPSession started");
    }


    @Override
    public void asapSessionFinished() {
        Log.d(this.getLogStart(), "ASAPSession finished");
    }
}
