package net.sharksystem.asap.android.example;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.asap.android.R;
// TODO do not work with internal classes in any example!!
import net.sharksystem.asap.android.apps.ASAPActivity;
import net.sharksystem.asap.engine.ASAPEngineFS;

import java.io.IOException;
import java.util.Set;

public class ASAPExampleActivity extends ASAPActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_layout);
    }

    public void onClick(View view) {
        View startWifiButton = findViewById(R.id.startWifiDirect);
        View stopWifiButton = findViewById(R.id.stopWifiDirect);
        View startBTButton = findViewById(R.id.startBT);
        View stopBTButton = findViewById(R.id.stopBT);
        View startLoRaButton = findViewById(R.id.startLoRa);
        View stopLoRaButton = findViewById(R.id.stopLoRa);

        if(view == startWifiButton) {
            Log.d(this.getLogStart(), "start wifi button pressed - send message");
            super.startWifiP2P();
        }
        else if(view == stopWifiButton) {
            Log.d(this.getLogStart(), "stop wifi button pressed - send message");
            super.stopWifiP2P();
        }
        else if(view == startBTButton) {
            Log.d(this.getLogStart(), "start bt button pressed - ask service to start bt");
            super.startBluetooth();
        }
        else if(view == stopBTButton) {
            Log.d(this.getLogStart(), "stop bt button pressed - send message");
            super.stopBluetooth();
        }
        /*
        else if(view == findViewById(R.id.startDiscoverable)) {
            Log.d(this.getLogStart(), "start discoverable button pressed - send message");
            this.startBluetoothDiscoverable();
        }
        else if(view == findViewById(R.id.startDiscovery)) {
            Log.d(this.getLogStart(), "start discover button pressed - send message");
            this.startBluetoothDiscovery();
        }
        */
        else if(view == findViewById(R.id.startDiscoverableAndDiscovery)) {
            Log.d(this.getLogStart(),
                    "start disoverable and discover button pressed - send messages");
            super.startBluetoothDiscovery();
            super.startBluetoothDiscoverable();
        }
        else if(view == startLoRaButton) {
            Log.d(this.getLogStart(), "start LoRa button pressed - send message");
            super.startLoRa();
        }
        else if(view == stopLoRaButton) {
            Log.d(this.getLogStart(), "stop LoRa button pressed - send message");
            super.stopLoRa();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    //                         changes in active layer 2 connections                         //
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void asapNotifyOnlinePeersChanged(Set<CharSequence> onlinePeerList) {
        super.asapNotifyOnlinePeersChanged(onlinePeerList);

        TextView peerListTextView = this.findViewById(R.id.onlinePeersList);

        if(onlinePeerList == null || onlinePeerList.size() < 1) {
            peerListTextView.setText("no peers online");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("peers online:");
            sb.append("\n");
            for(CharSequence peerID : onlinePeerList) {
                sb.append("id: ");
                sb.append(peerID);
                sb.append("\n");
            }
            peerListTextView.setText(sb.toString());
        }
        peerListTextView.refreshDrawableState();
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

    // TODO change that code - avoid internals interfaces!!
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

    public void onSwitch2ExchangeActivity(View view) {
        this.startActivity(new Intent(this, ASAPExampleMessagingActivity.class));
    }

    private void setupCleanASAPStorage() throws IOException, ASAPException {
        String absoluteFolderName = this.getASAPAndroidPeer().
                getApplicationRootFolder(ExampleAppDefinitions.ASAP_EXAMPLE_APPNAME);
        Log.d(this.getLogStart(), "going to clean folder:  " + absoluteFolderName);

        ASAPEngineFS.removeFolder(absoluteFolderName);

        Log.d(this.getLogStart(), "create asap storage with:  "
                + this.getASAPAndroidPeer().getOwnerID()
                + " | "
                + this.getASAPAndroidPeer().getApplicationRootFolder(ExampleAppDefinitions.ASAP_EXAMPLE_APPNAME)
                + " | "
                + ExampleAppDefinitions.ASAP_EXAMPLE_APPNAME
        );

        this.asapStorage = ASAPEngineFS.getASAPStorage(
                        this.getASAPAndroidPeer().getOwnerID().toString(),
                        this.getASAPAndroidPeer().getApplicationRootFolder(ExampleAppDefinitions.ASAP_EXAMPLE_APPNAME),
                ExampleAppDefinitions.ASAP_EXAMPLE_APPNAME);
    }

    public void onASAPHub(View view) {
        Log.d(this.getLogStart(), "onASAPHub reached");
        this.startActivity(new Intent(this, ASAPExampleHubManagementActivity.class));

        /*
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
         */
    }

    public void onRemoveAddOnlineSenderClick(View view) {
        Log.d(this.getLogStart(), "onRemoveAddOnlineSenderClick reached");
        Log.d(this.getLogStart(), "onAddOnlineSenderClick reached");

        /*
        if(this.asapOnlineSender == null) {
            Log.d(this.getLogStart(), "online message sender not set");
            return;
        }

        if(this.asapStorage == null) {
            Log.d(this.getLogStart(), "asap storage not set");
            return;
        }

        this.asapStorage.detachASAPMessageAddListener();
         */
    }

    private void checkStorage() throws IOException, ASAPException {
        if(this.asapStorage == null) {
            Log.d(this.getLogStart(), "storage not yet initialized - clean and setup:  " + MESSAGE);
            this.setupCleanASAPStorage();
        }
    }
}