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
import androidx.annotation.CallSuper;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.android.ASAPAndroid;
import net.sharksystem.asap.android.ASAPServiceMessage;
import net.sharksystem.asap.android.ASAPServiceMethods;
import net.sharksystem.asap.android.service.ASAPService;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceNotificationListener;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestListener;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyBroadcastReceiver;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ASAPActivity extends AppCompatActivity implements
        ASAPServiceRequestListener, ASAPServiceNotificationListener {

    private static final int MY_REQUEST_2ENABLE_BT = 1;
    private static final int MY_REQUEST_SET_BT_DISCOVERABLE = 2;

    private Messenger mService;
    private boolean mBound;

    private ASAPAndroidPeer asapAndroidPeer;

    protected ASAPAndroidPeer getASAPAndroidPeer() {
        if(!ASAPAndroidPeer.peerInitialized()) {
            Log.d(this.getLogStart(), "application side peer not yet initialized - try to restore from memory");
            Log.d(this.getLogStart(), "this == " + this);
            ASAPAndroidPeer.restoreFromMemento(this); // can throw exception
            Log.d(this.getLogStart(), "application side peer restored from memory");
        }

        this.asapAndroidPeer = ASAPAndroidPeer.getASAPAndroidPeer();

        return this.asapAndroidPeer;
    }

    protected ASAPPeer getASAPPeer() { return this.getASAPAndroidPeer(); }

    /**
     * Create a closed asap channel. Ensure to call this method before ever sending a message into
     * that channel. Any prior message would create an open channel with unrestricted recipient list.
     * (It sound worse than it is, though. Have a look at the concept description of ASAP.)
     * @param appName format / appName of your application and its ASAPEngine
     * @param uri describes content within your application
     * @param recipients list of recipients. Only peers on that list will even get this message.
     *                   This is more than setting access rights. It like a registered letter.
     *                   Peers which are not on the list will not even be aware of the existence
     *                   of this channel and it messages.
     * @throws ASAPException
     */
    public final void createClosedASAPChannel(CharSequence appName, CharSequence uri,
                    Collection<CharSequence> recipients) throws ASAPException {

        ASAPServiceMessage createClosedChannelMessage =
                ASAPServiceMessage.createCreateClosedChannelMessage(appName, uri, recipients);

        this.sendMessage2Service(createClosedChannelMessage.getMessage());
    }

    /**
     * Send asap message. If that channel does not exist: it will be created as open channel
     * (unrestricted recipient list). Closed channels must be created before
     * @param appName format / appName of your application and its ASAPEngine
     * @param uri describes content within your application
     * @param message application specific message as bytes.
     * @param persistent keep in an asap store and resent in following asap sessions
     * @throws ASAPException
     */
    public final void sendASAPMessage(CharSequence appName, CharSequence uri,
                    byte[] message, boolean persistent) throws ASAPException {
        Log.d(this.getLogStart(), "ask service to send: "
                + "format: " + appName
                + "| uri: " + uri
                + "| length: " + message.length);

        ASAPServiceMessage sendMessage =
                ASAPServiceMessage.createSendMessage(appName, uri, message, persistent);

        this.sendMessage2Service(sendMessage.getMessage());
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //                                 asap service requests                             //
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Application developer should neither call nor overwrite this message. It is called
     * as result of a start bluetooth request. We will hide it behind an interface in later
     * version. Until than: Please, do not call this method.
     */
    @Override
    @CallSuper
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

    /**
     * Application developer should neither call nor overwrite this message. It is called
     * as result of a start bluetooth request. We will hide it behind an interface in later
     * version. Until than: Please, do not call this method.
     */
    @Override
    @CallSuper
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

    /**
     * Application developer should neither call nor overwrite this message. It is the callback
     * method that is called if a broadcast is received. We will hide it behind an
     * interface in later
     * version. Until than: Please, do not call this method.
     */
    @CallSuper
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(this.getLogStart(),
                ".onActivityResult(): requestCode == " + requestCode +
                        " / resultCode == " + resultCode);

        if (requestCode == MY_REQUEST_2ENABLE_BT && resultCode == RESULT_OK) {
            Log.d(this.getLogStart(), "Bluetooth now enabled - ask service to start BT");
            this.sendMessage2Service(ASAPServiceMethods.START_BLUETOOTH);
        }

        if (requestCode == MY_REQUEST_SET_BT_DISCOVERABLE) {
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
//        return this.getClass().getSimpleName() +"->ASAPActivity";
        return this.getClass().getSimpleName();
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //                                 mac protocol stuff                              //
    /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Call this message to start Bluetooth.
     * asapNotifyBTEnvironmentStarted() is called later if BT could be started.
     */
    public void startBluetooth() {
        Log.d(this.getLogStart(), "send message to service: start BT");
        this.sendMessage2Service(ASAPServiceMethods.START_BLUETOOTH);
    }

    /**
     * Call this message to stop Bluetooth.
     * asapNotifyBTEnvironmentStopped() is called later if BT could be stopped
     */
    public void stopBluetooth() {
        Log.d(this.getLogStart(), "send message to service: stop BT");
        this.sendMessage2Service(ASAPServiceMethods.STOP_BLUETOOTH);
    }

    /**
     * Call this message to start Wifi direct.
     * Note: Wifi is not yet fully supported. Do not use this method yet.
     */
    public void startWifiP2P() {
        Log.d(this.getLogStart(), "send message to service: start Wifi P2P");
        this.sendMessage2Service(ASAPServiceMethods.START_WIFI_DIRECT);
    }

    /**
     * Call this message to stop Wifi direct.
     * Note: Wifi is not yet fully supported. Do not use this method yet.
     */
    public void stopWifiP2P() {
        Log.d(this.getLogStart(), "send message to service: stop Wifi P2P");
        this.sendMessage2Service(ASAPServiceMethods.STOP_WIFI_DIRECT);
    }

    /**
     * Call this message to start LoRa.
     */
    public void startLoRa() {
        Log.d(this.getLogStart(), "send message to service: start LoRa");
        this.sendMessage2Service(ASAPServiceMethods.START_LORA);
    }

    /**
     * Call this message to stop LoRa.
     */
    public void stopLoRa() {
        Log.d(this.getLogStart(), "send message to service: stop LoRa");
        this.sendMessage2Service(ASAPServiceMethods.STOP_LORA);
    }

    /**
     * Call this message to make this device discoverable with Bluetooth.
     * asapNotifyBTDiscoverableStarted() is called later.
     * There is not matching stop method. Bluetooth discoverability is stopped after some time
     * from Android. asapNotifyBTDiscoverableStopped() is called in this case.
     *
     */
    public void startBluetoothDiscoverable() {
        Log.d(this.getLogStart(), "send message to service: start BT Discoverable");
        this.sendMessage2Service(ASAPServiceMethods.START_BLUETOOTH_DISCOVERABLE);
    }

    /**
     * Call this message to start Bluetooth discovery. This device will look for discoverable
     * Bluetooth devices.
     * asapNotifyBTDiscoveryStarted() is called later.
     * There is not matching stop method. Bluetooth discovery is stopped after some time
     * from Android. asapNotifyBTDiscoveryStopped() is called in this case.
     *
     */
    public void startBluetoothDiscovery() {
        Log.d(this.getLogStart(), "send message to service: start BT Discovery");
        this.sendMessage2Service(ASAPServiceMethods.START_BLUETOOTH_DISCOVERY);
    }

    /*
    There is a race condition:
    ASAPActivity a can launch ASAPActivity b. It happens (quite often actually) that a.onStop()
    is processed after b.onStart() or b.onResume(). In that case, startASAPBroadcast are called
    twice in the row (a.onStart() sometimes earlier, than b.onStart()) followed by
    stopASAPBroadcast issued by a.onStop(). b would not receive any broadcast - ASAP service
    was told to stop to send broadcasts.

    Solution: We only stop broadcasting if there if the last activity signs off.
     */
    private void startASAPEngineBroadcasts() {
        Log.d(this.getLogStart(), "send message to service: start ASAP Engine Broadcasts");
        this.sendMessage2Service(ASAPServiceMethods.START_BROADCASTS);
    }

    private void stopASAPEngineBroadcasts() {
        if(this.getASAPAndroidPeer().getNumberASAPActivities() == 1) {
            Log.d(this.getLogStart(), "send message to service: stop ASAP Engine Broadcasts");
            this.sendMessage2Service(ASAPServiceMethods.STOP_BROADCASTS);
        } else {
            Log.d(this.getLogStart(), "don't stop broadcasts there are still listeners");
        }
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
        filter.addAction(ASAPAndroid.ASAP_CHUNK_RECEIVED_ACTION);

        // register
        this.registerReceiver(this.getASAPAndroidPeer(), filter);
    }

    private void shutdownASAPChunkReceivedBroadcastReceiver() {
        Log.d(this.getLogStart(), "shutdown asap received bc receiver");
        try {
            this.unregisterReceiver(this.getASAPAndroidPeer());
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
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(this.getLogStart(), "onCreate");
        this.getASAPAndroidPeer().activityCreated(this);
    }

    protected void onStart() {
        // Bind to the service
        super.onStart();
        Log.d(this.getLogStart(), "onStart");
        this.getASAPAndroidPeer().setActivity(this);
        this.setupASAPServiceNotificationBroadcastReceiver();
        this.setupASAPChunkReceivedBroadcastReceiver();
        this.bindServices();
        // (re-)start asap broadcast request is issued whenever connection was established
        // see ServiceConnection below
    }

    protected void onResume() {
        super.onResume();
        Log.d(this.getLogStart(), "onResume");
        this.getASAPAndroidPeer().setActivity(this);
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
        this.getASAPAndroidPeer().activityDestroyed(this);

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
                Log.d(getLogStart(), "send stored service messages | #msg = " + messageStorage.size());
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

    /**
     * Application developers can use this method like life-cycle methods in Android
     * (onStart() etc.). Overwrite this method to get informed about changes in environment.
     * Do not call this method yourself. Do not forget to call the super method if you overwrite.
     *
     *  <br/><br/>
     *  This method informs that this device is now a discoverable Bluetooth device
     */
    @Override
    @CallSuper
    public void asapNotifyBTDiscoverableStarted() {
        this.getASAPAndroidPeer().notifyBTDiscoverable(true);
    }

    /**
     * Application developers can use this method like life-cycle methods in Android
     * (onStart() etc.). Overwrite this method to get informed about changes in environment.
     * Do not call this method yourself. Do not forget to call the super method if you overwrite.
     *
     *  <br/><br/>
     *  This method informs that this device is no longer a discoverable Bluetooth device
     */
    @Override
    @CallSuper
    public void asapNotifyBTDiscoverableStopped() {
        this.getASAPAndroidPeer().notifyBTDiscoverable(false);
    }

    /**
     * Application developers can use this method like life-cycle methods in Android
     * (onStart() etc.). Overwrite this method to get informed about changes in environment.
     * Do not call this method yourself. Do not forget to call the super method if you overwrite.
     *
     *  <br/><br/>
     *  This method informs that bluetooth is enabled now.
     */
    @Override
    @CallSuper
    public void asapNotifyBTEnvironmentStarted() {
        this.getASAPAndroidPeer().notifyBTEnvironmentRunning(true);
    }

    /**
     * Application developers can use this method like life-cycle methods in Android
     * (onStart() etc.). Overwrite this method to get informed about changes in environment.
     * Do not call this method yourself. Do not forget to call the super method if you overwrite.
     *
     *  <br/><br/>
     *  This method informs that bluetooth is disabled now.
     */
    @Override
    @CallSuper
    public void asapNotifyBTEnvironmentStopped() {
        this.getASAPAndroidPeer().notifyBTEnvironmentRunning(false);
    }

    /**
     * Application developers can use this method like life-cycle methods in Android
     * (onStart() etc.). Overwrite this method to get informed about changes in environment.
     * Do not call this method yourself. Do not forget to call the super method if you overwrite.
     *
     *  <br/><br/>
     *  This method called whenever list of connected peers changed. This happens when
     *  a connection is created or broken. The current list of peers comes as parameter.
     * @param peerList
     */
    @Override
    @CallSuper
    public void asapNotifyOnlinePeersChanged(Set<CharSequence> peerList) {
        this.getASAPAndroidPeer().notifyOnlinePeersChanged(peerList);
    }

    /**
     * Application developers can use this method like life-cycle methods in Android
     * (onStart() etc.). Overwrite this method to get informed about changes in environment.
     * Do not call this method yourself. Do not forget to call the super method if you overwrite.
     *
     *  <br/><br/>
     *  This method informs that bluetooth discovery started.
     */
    @Override
    @CallSuper
    public void asapNotifyBTDiscoveryStarted() {
        this.getASAPAndroidPeer().notifyBTDiscovery(true);
    }

    /**
     * Application developers can use this method like life-cycle methods in Android
     * (onStart() etc.). Overwrite this method to get informed about changes in environment.
     * Do not call this method yourself. Do not forget to call the super method if you overwrite.
     *
     *  <br/><br/>
     *  This method informs that bluetooth discovery stopped.
     */
    @Override
    @CallSuper
    public void asapNotifyBTDiscoveryStopped() {
        this.getASAPAndroidPeer().notifyBTDiscovery(false);
    }

    ////////////////////////////////////////////////////////////////////////////////
    //                                status methods                              //
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @return true if bluetooth environment is one.
     */
    public boolean isBluetoothEnvironmentOn() {
        return this.getASAPAndroidPeer().getBTEnvironmentRunning();
    }

    /**
     * @return true if this device is discoverable now.
     */
    public boolean isBluetoothDiscoverable() {
        return this.getASAPAndroidPeer().getBTDiscoverable();
    }

    /**
     * @return true if this device looking for other Bluetooth devices.
     */
    public boolean isBluetoothDiscovery() {
        return this.getASAPAndroidPeer().getBTDiscovery();
    }
}
