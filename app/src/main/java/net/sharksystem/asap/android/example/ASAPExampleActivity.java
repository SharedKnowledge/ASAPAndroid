package net.sharksystem.asap.android.example;

import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import net.sharksystem.asap.ASAPEngineFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.asap.android.ASAP;
import net.sharksystem.asap.android.apps.ASAPActivity;
import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPApplication;
import net.sharksystem.asap.android.apps.ASAPMessageReceivedListener;
import net.sharksystem.asap.android.apps.ASAPOnlineMessageSenderAndroidUserSide;
import net.sharksystem.asap.apps.ASAPMessages;

import java.io.IOException;

public class ASAPExampleActivity extends ASAPActivity implements ASAPMessageReceivedListener {
    private static final CharSequence TESTURI ="asap://testuri";
    private static final CharSequence TESTMESSAGE = "Hi there from asap writing activity";
    private ASAPOnlineMessageSenderAndroidUserSide asapOnlineSender;

    public ASAPExampleActivity() {
        super(ASAPApplication.getASAPApplication());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // add as message received listener
        this.getASAPApplication().addASAPMessageReceivedListener(URI, this);

        // create broadcast receiver
        ExampleASAPBroadcastReceiver br = new ExampleASAPBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ASAP.ASAP_CHUNK_RECEIVED_ACTION);
        this.registerReceiver(br, filter);
    }

    public void onClick(View view) {
        View startWifiButton = findViewById(R.id.startWifiDirect);
        View stopWifiButton = findViewById(R.id.stopWifiDirect);
        View startBTButton = findViewById(R.id.startBT);
        View stopBTButton = findViewById(R.id.stopBT);

        if(view == startWifiButton) {
            Log.d(this.getLogStart(), "start wifi button pressed - send message");
            this.startWifiP2P();
        }
        else if(view == stopWifiButton) {
            Log.d(this.getLogStart(), "stop wifi button pressed - send message");
            this.stopWifiP2P();
        }
        else if(view == startBTButton) {
            Log.d(this.getLogStart(), "start bt button pressed - ask service to start bt");
            this.startBluetooth();
        }
        else if(view == stopBTButton) {
            Log.d(this.getLogStart(), "stop bt button pressed - send message");
            this.stopBluetooth();
        }
        else if(view == findViewById(R.id.startDiscoverable)) {
            Log.d(this.getLogStart(), "start discoverable button pressed - send message");
            this.startBluetoothDiscoverable();
        }
        else if(view == findViewById(R.id.startDiscovery)) {
            Log.d(this.getLogStart(), "start discover button pressed - send message");
            this.startBluetoothDiscovery();
        }
        else if(view == findViewById(R.id.startDiscoverableAndDiscovery)) {
            Log.d(this.getLogStart(),
                    "start disoverable and discover button pressed - send messages");
            this.startBluetoothDiscovery();
            this.startBluetoothDiscoverable();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    //                      asap message receiver example implementation                     //
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void asapMessagesReceived(ASAPMessages messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("received: | ");
        sb.append(messages.getFormat());
        sb.append( "| ");
        sb.append(messages.getURI());
        Log.d(this.getLogStart(), "asap message arrived: " + sb.toString());
        Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    //                                         helps debugging                               //
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void asapNotifyBTDiscoverableStopped() {
        super.asapNotifyBTDiscoverableStopped();
        Log.d(this.getLogStart(), "got notified: discoverable stopped");
        Toast.makeText(this, "discoverable stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void asapNotifyBTDiscoveryStopped() {
        super.asapNotifyBTDiscoveryStopped();
        Log.d(this.getLogStart(), "got notified: discovery stopped");
        Toast.makeText(this, "discovery stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void asapNotifyBTDiscoveryStarted() {
        super.asapNotifyBTDiscoveryStarted();
        Log.d(this.getLogStart(), "got notified: discovery started");
        Toast.makeText(this, "discovery started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void asapNotifyBTDiscoverableStarted() {
        super.asapNotifyBTDiscoverableStarted();
        Log.d(this.getLogStart(), "got notified: discoverable started");
        Toast.makeText(this, "discoverable started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void asapNotifyBTEnvironmentStarted() {
        super.asapNotifyBTEnvironmentStarted();
        Log.d(this.getLogStart(), "got notified: bluetooth on");
        Toast.makeText(this, "bluetooth on", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void asapNotifyBTEnvironmentStopped() {
        super.asapNotifyBTEnvironmentStopped();
        Log.d(this.getLogStart(), "got notified: bluetooth off");
        Toast.makeText(this, "bluetooth off", Toast.LENGTH_SHORT).show();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    //                              asap store test scenario(s)                              //
    ///////////////////////////////////////////////////////////////////////////////////////////

    private final String APPNAME = "ASAP_TEST_APP";
    private final String URI = "sn://chat";
    private final String MESSAGE = "Hi, that's a message";
    private final byte[] BYTE_MESSAGE = MESSAGE.getBytes();

    ASAPStorage asapStorage;

    public void onSetupCleanASAPStorageClick(View view) {
        try {
            this.setupCleanASAPStorage();
        }
        catch (IOException | ASAPException e) {
            Log.d(this.getLogStart(), "exception: " + e.getLocalizedMessage());
        }
        catch (RuntimeException e) {
            Log.d(this.getLogStart(), "runtime exception: " + e.getLocalizedMessage());
        }
    }

    public void onAddASAPMessageClick(View view) {
        try {
            this.addMessage();
        }
        catch (IOException | ASAPException e) {
            Log.d(this.getLogStart(), "exception: " + e.getLocalizedMessage());
        }
        catch (RuntimeException e) {
            Log.d(this.getLogStart(), "runtime exception: " + e.getLocalizedMessage());
        }
    }

    private void setupCleanASAPStorage() throws IOException, ASAPException {
        String absoluteFolderName = this.getASAPApplication().getApplicationRootFolder(APPNAME);
        Log.d(this.getLogStart(), "going to clean folder:  " + absoluteFolderName);

        ASAPEngineFS.removeFolder(absoluteFolderName);

        Log.d(this.getLogStart(), "create asap storage with:  "
                + this.getASAPApplication().getASAPOwner()
                + " | "
                + this.getASAPApplication().getApplicationRootFolder(APPNAME)
                + " | "
                + APPNAME
        );

        this.asapStorage = ASAPEngineFS.getASAPStorage(
                        this.getASAPApplication().getASAPOwner().toString(),
                        this.getASAPApplication().getApplicationRootFolder(APPNAME),
                        APPNAME);
    }

    public void onAddOnlineSenderClick(View view) {
        Log.d(this.getLogStart(), "onAddOnlineSenderClick reached");
        try {
            this.checkStorage();
            if(asapOnlineSender != null) {
                Log.d(this.getLogStart(), "online message sender already set");
                return;
            }

            this.asapOnlineSender = new ASAPOnlineMessageSenderAndroidUserSide(this.getASAPApplication());
            this.asapStorage.attachASAPMessageAddListener(this.asapOnlineSender);
        } catch (IOException | ASAPException e) {
            Log.d(this.getLogStart(), "exception: " + e.getLocalizedMessage());
        }
    }

    public void onRemoveAddOnlineSenderClick(View view) {
        Log.d(this.getLogStart(), "onRemoveAddOnlineSenderClick reached");

        if(this.asapOnlineSender == null) {
            Log.d(this.getLogStart(), "online message sender not set");
            return;
        }

        if(this.asapStorage == null) {
            Log.d(this.getLogStart(), "asap storage not set");
            return;
        }

        this.asapStorage.detachASAPMessageAddListener(this.asapOnlineSender);
    }

    private void checkStorage() throws IOException, ASAPException {
        if(this.asapStorage == null) {
            Log.d(this.getLogStart(), "storage not yet initialized - clean and setup:  " + MESSAGE);
            this.setupCleanASAPStorage();
        }
    }

    private void addMessage() throws IOException, ASAPException {
        // indirect - prefered way - send via ASAPService
        Log.d(this.getLogStart(), "ask asap service to deliver a message");
        this.sendASAPMessage(APPNAME, URI, null, BYTE_MESSAGE);

        /*
        // direct approach - write into local file system
        this.checkStorage();
        Log.d(this.getLogStart(), "add message to storage:  " + MESSAGE);
        this.asapStorage.add(URI, MESSAGE);
         */
    }
}