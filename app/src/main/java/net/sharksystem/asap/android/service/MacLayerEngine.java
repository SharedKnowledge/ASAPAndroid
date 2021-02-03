package net.sharksystem.asap.android.service;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.protocol.ASAPConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public abstract class MacLayerEngine {
    // TODO - create a real UUID.
    public static final UUID ASAP_UUID = UUID.fromString("42ba5e3b-d4f0-4578-91dc-8dab6c2067ae");
    public static final String ASAP_SERVICE_NAME = "ASAP";

    private final ASAPService asapService;
    private final Context context;

//    public static final long DEFAULT_WAIT_BEFORE_RECONNECT_TIME = 1000*60; // a minute
    public static final long DEFAULT_WAIT_BEFORE_RECONNECT_TIME = 1000; // a second - debugging
    private final long waitBeforeReconnect;
    private final int randomValue; // to avoid a nasty race condition

    public MacLayerEngine(ASAPService asapService, Context context) {
        this(asapService, context, DEFAULT_WAIT_BEFORE_RECONNECT_TIME);
    }

    public MacLayerEngine(ASAPService asapService, Context context, long waitingPeriod) {
        this.asapService = asapService;
        this.context = context;
        this.waitBeforeReconnect = waitingPeriod;

        Random random = new Random(System.currentTimeMillis());
        this.randomValue = random.nextInt();
    }

    protected Context getContext() {
        return this.context;
    }

    protected ASAPService getAsapService() {
        return this.asapService;
    }

    public abstract void start();
    public abstract void stop();

    /** try to reconnect to previously met (paired devices)
     * @return true if environment was up and running, false.. if in general impossible to reconnect
     */
    public abstract boolean tryReconnect();

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

    private Map<String, ASAPConnection> asapConnections = new HashMap<>();

    private String localMacAddress = null;
    public String getLocalMacAddress() {
        if(localMacAddress == null) {
            WifiManager wifiManager = (WifiManager)
                    asapService.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            WifiInfo wInfo = wifiManager.getConnectionInfo();
            this.localMacAddress = wInfo.getMacAddress();
        }

        return this.localMacAddress;
    }

    /**
     * kill connection to address
     * @param address
     */
    protected void kill(String address) {
        ASAPConnection asapConnection = this.asapConnections.remove(address);
        if(asapConnection != null) {
            Log.d(this.getLogStart(), "going kill connection to: " + address);
            asapConnection.kill();
        } else {
            Log.d(this.getLogStart(), "no connection to kill to: " + address);
        }
    }

    protected void launchASAPConnection(
            String address, InputStream inputStream, OutputStream outputStream) {

        Log.d(this.getLogStart(), "going to launch a new asap connection");

        try {
            Log.d(this.getLogStart(), "call asap peer to handle connection");
//            TestConnectionHandler testConnectionHandler = new TestConnectionHandler(this.is, this.os);
//            testConnectionHandler.start();
            this.asapConnections.put(address,
                this.getAsapService().getASAPPeer().handleConnection(inputStream, outputStream));

        } catch (IOException | ASAPException e) {
            Log.d(this.getLogStart(), "while launching asap connection: " + e.getLocalizedMessage());
        }
    }

    /** There is a race condition which is not that obvious: Example Bluetooth
     Bot phones A and B are going to initiate a connection. Both also offer a server socket.
     Assumed, A and B initiate a connection in the same moment (create a client socket).
     Both would get a connection and launch an ASAP session. If the timing is bad - and that's
     more likely as I wished - both would be asked from their server sockets to handle a
     new connection as well.

     In principle, that is what we want. With a bad timing that would happen on both sides, though.
     In that case, both would realize that there is already an existing communication channel
     (their own TCP client socket) and close the server side socket. Again, that is what we
     want - but not on both ends.

     Both connections would be killed. We want one TCP channel to be closed. But only one.

     Solution: We implement a bias: This method is called with an additional parameter (initiator).
     In TCP, we could call it client socket. Connection was initiated by creating a TCP port. It can be used
     in any way but it must be ensured: Both side will not use same boolean value
     */
    public boolean waitBeforeASAPSessionLaunch(InputStream is, OutputStream os,
                               boolean connectionInitiator, long waitInMillis) throws IOException {
        // run a little negotiation before we start
        DataOutputStream dos = new DataOutputStream(os);
        int remoteValue = 0;

        try {
            dos.writeInt(this.randomValue);
            DataInputStream dis = new DataInputStream(is);
            remoteValue = dis.readInt();
        } catch (IOException e) {
            // decision is made - this connection is gone anyway
            os.close();
            is.close();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("try to solve race condition: localValue == ");
        sb.append(this.randomValue);
        sb.append(" | remoteValue == ");
        sb.append(remoteValue);
        sb.append(" | initiator == ");
        sb.append(connectionInitiator);

        int initiatorValue, nonInitiatorValue;
        if(connectionInitiator) {
            initiatorValue = this.randomValue;
            nonInitiatorValue = remoteValue;
        } else {
            initiatorValue = remoteValue;
            nonInitiatorValue = this.randomValue;
        }

        sb.append(" | initiatorValue == ");
        sb.append(initiatorValue);
        sb.append(" | nonInitiatorValue == ");
        sb.append(nonInitiatorValue);
        Log.d(this.getLogStart(), sb.toString());

        /* Here comes the bias: An initiator with a smaller value waits a moment */
        if(connectionInitiator & initiatorValue < nonInitiatorValue) {
            try {
                sb = new StringBuilder();
                sb.append("wait ");
                sb.append(waitInMillis);
                sb.append(" ms");
                Log.d(this.getLogStart(), sb.toString());
                Thread.sleep(waitInMillis);
                return true;
            } catch (InterruptedException e) {
                Log.d(this.getLogStart(), "wait interrupted");
            }
        }

        return false;
    }

    /**
     * TODO: do we need this? I doubt it (thsc)
     * It can be called to check whether open connection are still running. It's a good idea for
     * all connection oriented protocols but useless with connectionless.
     */
    public abstract void checkConnectionStatus();
}
