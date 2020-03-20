package net.sharksystem.asap.android.apps;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.ASAP;
import net.sharksystem.asap.android.ASAPServiceMessage;
import net.sharksystem.asap.android.ASAPServiceMethods;
import net.sharksystem.asap.android.service.ASAPService;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceNotificationListener;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestListener;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyBroadcastReceiver;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;
import net.sharksystem.asap.util.Helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ASAPActivity extends AppCompatActivity implements
        ASAPServiceRequestListener, ASAPServiceNotificationListener {

    private static final int MY_REQUEST_2ENABLE_BT = 1;
    private static final int MY_REQUEST_SET_BT_DISCOVERABLE = 2;

    private Messenger mService;
    private boolean mBound;

    private ASAPApplication asapApplication;

    public ASAPActivity(ASAPApplication asapApplication) {
        this.asapApplication = asapApplication;
    }

    protected ASAPApplication getASAPApplication() {
        return this.asapApplication;
    }

    /**
     * Create a closed asap channel. Ensure to call this method before ever sending a message into
     * that channel.
     * @param appName
     * @param uri
     * @param recipients
     * @throws ASAPException
     */
    public void createClosedASAPChannel(CharSequence appName, CharSequence uri,
                    Collection<CharSequence> recipients) throws ASAPException {

        ASAPServiceMessage createClosedChannelMessage =
                ASAPServiceMessage.createCreateClosedChannelMessage(appName, uri, recipients);

        this.sendMessage2Service(createClosedChannelMessage.getMessage());
    }

    /**
     * Send asap message. If that channel does not exist: it will be created as open channel
     * (unrestricted recipient list). Closed channels must be created before
     * @param appName
     * @param uri
     * @param message
     * @param persistent keep in an asap store and resent in following asap sessions
     * @throws ASAPException
     */
    public void sendASAPMessage(CharSequence appName, CharSequence uri,
                    byte[] message, boolean persistent) throws ASAPException {

        ASAPServiceMessage sendMessage =
                ASAPServiceMessage.createSendMessage(appName, uri, message, persistent);

        this.sendMessage2Service(sendMessage.getMessage());
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //                                 asap service requests                             //
    ///////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void asapSrcRq_enableBluetooth() {
        // check if bt is enabled
        // get default bt adapter - there could be proprietary adapters
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter == null) {
            // Device doesn't support Bluetooth
            Log.i(this.getLogStart(), "device does not support bluetooth - give up");
            Toast t = Toast.makeText(this,
                    "Device does not support Bluetooth - give up", Toast.LENGTH_LONG);
            t.show();
            return;
        }

        Log.d(this.getLogStart(), "device has a bluetooth adapter");

        // those things are to be done in calling activity
        if (!defaultAdapter.isEnabled()) {
            Log.d(this.getLogStart(), "BT disabled - ask user to enable");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, MY_REQUEST_2ENABLE_BT);
            // issued - wait for reply
            return;
        }
    }

    private int visibilityTime = 1;

    @Override
    public void asapSrcRq_startBTDiscoverable(int time) {
        this.visibilityTime = time;
        Log.d(this.getLogStart(), "going to make device bt visibile for seconds: "
                + this.visibilityTime);

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                this.visibilityTime);

        // ask user to confirm - result is passed to onActivityResult
        this.startActivityForResult(discoverableIntent, MY_REQUEST_SET_BT_DISCOVERABLE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(this.getLogStart(),
                ".onActivityResult(): requestCode == " + requestCode +
                        " / resultCode == " + resultCode);

        if(requestCode == MY_REQUEST_2ENABLE_BT && resultCode == RESULT_OK) {
            Log.d(this.getLogStart(), "Bluetooth now enabled - ask service to start BT");
            this.sendMessage2Service(ASAPServiceMethods.START_BLUETOOTH);
        }

        if(requestCode == MY_REQUEST_SET_BT_DISCOVERABLE) {
            Log.d(this.getLogStart(),
                    "user allowed BT discoverability for seconds: "
                        + resultCode);

            // notify
            Log.d(this.getLogStart(), "call asapNotifyBTDiscoverableStarted()");
            this.asapNotifyBTDiscoverableStarted();
        }
    }

    private List<Message> messageStorage = null;
    protected void sendMessage2Service(int messageNumber) {
        ASAPServiceMessage asapServiceMessage = ASAPServiceMessage.createMessage(messageNumber);
        Message msg = asapServiceMessage.getMessage();

        if(this.mService == null) {
            Log.d(this.getLogStart(), "service not yet available - cannot send but store message");
            if(this.messageStorage == null) {
                this.messageStorage  = new ArrayList<Message>();
            }
            this.messageStorage.add(msg);
        } else {
            this.sendMessage2Service(msg);
        }
    }

    public void sendMessage2Service(Message msg) {
        try {
            this.mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected String getLogStart() {
        return this.getClass().getSimpleName();
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //                                 mac protocol stuff                              //
    /////////////////////////////////////////////////////////////////////////////////////

    public void startBluetooth() {
        Log.d(this.getLogStart(), "send message to service: start BT");
        this.sendMessage2Service(ASAPServiceMethods.START_BLUETOOTH);
    }

    public void stopBluetooth() {
        Log.d(this.getLogStart(), "send message to service: stop BT");
        this.sendMessage2Service(ASAPServiceMethods.STOP_BLUETOOTH);
    }

    public void startWifiP2P() {
        Log.d(this.getLogStart(), "send message to service: start Wifi P2P");
        this.sendMessage2Service(ASAPServiceMethods.START_WIFI_DIRECT);
    }

    public void stopWifiP2P() {
        Log.d(this.getLogStart(), "send message to service: stop Wifi P2P");
        this.sendMessage2Service(ASAPServiceMethods.STOP_WIFI_DIRECT);
    }

    public void startBluetoothDiscoverable() {
        Log.d(this.getLogStart(), "send message to service: start BT Discoverable");
        this.sendMessage2Service(ASAPServiceMethods.START_BLUETOOTH_DISCOVERABLE);
    }

    public void startBluetoothDiscovery() {
        Log.d(this.getLogStart(), "send message to service: start BT Discovery");
        this.sendMessage2Service(ASAPServiceMethods.START_BLUETOOTH_DISCOVERY);
    }

    public void startASAPEngineBroadcasts() {
        Log.d(this.getLogStart(), "send message to service: start ASAP Engine Broadcasts");
        this.sendMessage2Service(ASAPServiceMethods.START_BROADCASTS);
    }

    public void stopASAPEngineBroadcasts() {
        Log.d(this.getLogStart(), "send message to service: stop ASAP Engine Broadcasts");
        this.sendMessage2Service(ASAPServiceMethods.STOP_BROADCASTS);
    }

    public void refreshProtocolStatus() {
        Log.d(this.getLogStart(), "send message to ask for protocol status broadcasts");
        this.sendMessage2Service(ASAPServiceMethods.ASK_PROTOCOL_STATUS);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //            asap service notification broadcast receiver management              //
    /////////////////////////////////////////////////////////////////////////////////////

    private ASAPServiceRequestNotifyBroadcastReceiver srbc;

    private void setupASAPServiceNotificationBroadcastReceiver() {
        Log.d(this.getLogStart(), "setup asap service notification bc receiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ASAPServiceRequestNotifyIntent.ASAP_SERVICE_REQUEST_ACTION);

        // create new bc receiver - register this - whatever it is - as listener
        this.srbc = new ASAPServiceRequestNotifyBroadcastReceiver(
                this, this);

        // register
        this.registerReceiver(this.srbc, filter);
    }

    private void shutdownASAPServiceNotificationBroadcastReceiver() {
        Log.d(this.getLogStart(), "shutdown asap service rq / notification bc receiver");
        if(this.srbc == null) {
            Log.e(this.getLogStart(), "bc receiver is null");
            return;
        }

        try {
            this.unregisterReceiver(this.srbc);
        }
        catch(RuntimeException re) {
            Log.d(this.getLogStart(), "problems when unregister asap br rc - ignore"
                    + re.getLocalizedMessage());
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //                        asap received broadcast management                       //
    /////////////////////////////////////////////////////////////////////////////////////

    // asap application handles message from asap engine
    private void setupASAPChunkReceivedBroadcastReceiver() {
        Log.d(this.getLogStart(), "setup asap received bc receiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ASAP.ASAP_CHUNK_RECEIVED_ACTION);

        // register
        this.registerReceiver(this.asapApplication, filter);
    }

    private void shutdownASAPChunkReceivedBroadcastReceiver() {
        Log.d(this.getLogStart(), "shutdown asap received bc receiver");
        try {
            this.unregisterReceiver(this.asapApplication);
        }
        catch(RuntimeException re) {
            Log.d(this.getLogStart(), "problems when unregister asap received bcr - ignore"
                    + re.getLocalizedMessage());
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //                                       life cycle                                //
    /////////////////////////////////////////////////////////////////////////////////////

    /*
    There are three things to manage
    a) service binding - multiple activities can be bound to a single asap service
    They must be unbind when stopped, though
    b) asap service request / notification service and
    c) asap engine broadcast that notifies about new information arrival
    Both must be stopped whenever an activity gets inactive and re-launched
     */

    /** currently, we have only two stats: on and off */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(this.getLogStart(), "onCreate");
        this.asapApplication.activityCreated(this, this.initASAPApplication);
    }

    private boolean initASAPApplication;

    protected void setInitASAPApplication(boolean init) {
        this.initASAPApplication = init;
    }

    protected void onStart() {
        // Bind to the service
        super.onStart();
        Log.d(this.getLogStart(), "onStart");
        this.asapApplication.setActivity(this);
        this.setupASAPServiceNotificationBroadcastReceiver();
        this.setupASAPChunkReceivedBroadcastReceiver();
        this.bindServices();
        // (re-)start asap broadcast request is issued whenever connection was established
        // see ServiceConnection below
    }

    protected void onResume() {
        super.onResume();
        Log.d(this.getLogStart(), "onResume");
        this.asapApplication.setActivity(this);
        this.startASAPEngineBroadcasts();
    }

    protected void onPause() {
        super.onPause();
        Log.d(this.getLogStart(), "onPause");
        this.stopASAPEngineBroadcasts();
    }

    protected void onStop() {
        // Unbind from the service
        super.onStop();
        Log.d(this.getLogStart(), "onStop");
        this.shutdownASAPServiceNotificationBroadcastReceiver();
        this.shutdownASAPChunkReceivedBroadcastReceiver();
        this.unbindServices();

        // forget stored messages
        this.messageStorage = null;
        // stop protocols?
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(this.getLogStart(), "onDestroy");
        this.shutdownASAPServiceNotificationBroadcastReceiver();
        this.unbindServices();
        this.asapApplication.activityDestroyed(this);

        // forget stored messages
        this.messageStorage = null;

        /*
        this.sendMessage2Service(ASAPServiceMethods.STOP_WIFI_DIRECT);
        this.sendMessage2Service(ASAPServiceMethods.STOP_BLUETOOTH);
        this.sendMessage2Service(ASAPServiceMethods.STOP_BROADCASTS);

        // and kill service itself
        this.activity.stopService(new Intent(this.activity, ASAPService.class));
         */
    }

    private void bindServices() {
        this.bindService(new Intent(this, ASAPService.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindServices() {
        if (mBound) {
            try {
                this.unbindService(mConnection);
            }
            catch(RuntimeException re) {
                Log.d(this.getLogStart(), "exception when trying to unbind: "
                        + re.getLocalizedMessage());
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;

            Log.d(getLogStart(), "asap activity got connected to asap service");
            if(messageStorage != null && messageStorage.size() > 0) {
                Log.d(getLogStart(), "send stored messages | #msg = " + messageStorage.size());
                for(Message msg : messageStorage) {
                    sendMessage2Service(msg);
                }

                messageStorage = null;
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };

    @Override
    public void asapNotifyBTDiscoverableStarted() {
        this.asapApplication.setBTDiscoverable(true);
    }

    @Override
    public void asapNotifyBTDiscoverableStopped() {
        this.asapApplication.setBTDiscoverable(false);
    }

    @Override
    public void asapNotifyBTEnvironmentStarted() {
        this.asapApplication.setBTEnvironmentRunning(true);
    }

    @Override
    public void asapNotifyBTEnvironmentStopped() {
        this.asapApplication.setBTEnvironmentRunning(false);
    }

    @Override
    public void asapNotifyOnlinePeersChanged(List<CharSequence> peerList) {
        this.asapApplication.setOnlinePeersList(peerList);
    }

    @Override
    public void asapNotifyBTDiscoveryStarted() {
        this.asapApplication.setBTDiscovery(true);
    }

    @Override
    public void asapNotifyBTDiscoveryStopped() {
        this.asapApplication.setBTDiscovery(false);
    }

    ////////////////////////////////////////////////////////////////////////////////
    //                                status methods                              //
    ////////////////////////////////////////////////////////////////////////////////

    public boolean isBluetoothEnvironmentOn() {
        return this.asapApplication.getBTEnvironmentRunning();
    }

    public boolean isBluetoothDiscoverable() {
        return this.asapApplication.getBTDiscoverable();
    }

    public boolean isBluetoothDiscovery() {
        return this.asapApplication.getBTDiscovery();
    }
}
