package net.sharksystem.asap.android.example;

import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPActivity;
import net.sharksystem.asap.android.apps.HubConnectionManagerApplicationSide;
import net.sharksystem.asap.android.apps.HubManagerStatusChangedListener;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;
import net.sharksystem.hub.ASAPHubException;
import net.sharksystem.hub.HubConnectionManager;
import net.sharksystem.hub.peerside.HubConnectorDescription;
import net.sharksystem.hub.peerside.TCPHubConnectorDescriptionImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ASAPExampleHubTesterActivity extends ASAPActivity implements HubManagerStatusChangedListener {
    private TextView messageTextView;
    private ASAPMessageReceivedListener receivedListener;
    private HubConnectionManagerApplicationSide hubConnectionManager;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_hub_tester_layout);

        listView = findViewById(R.id.listViewAvailableHubs);
        messageTextView = findViewById(R.id.textViewReceivedASAPMessages);
        hubConnectionManager = (HubConnectionManagerApplicationSide) this.getHubConnectionManager();
        hubConnectionManager.addListener(this);
        this.receivedListener = new ASAPMessageReceivedListener() {
            @Override
            public void asapMessagesReceived(ASAPMessages asapMessages,
                                             String senderE2E, // E2E part
                                             List<ASAPHop> asapHops) {
                ASAPExampleHubTesterActivity.this.doHandleReceivedMessages(asapMessages);
            }
        };
    }

    private void hubAction(boolean on) {
        EditText hostnameET = findViewById(R.id.hostname);
        String hostName = hostnameET.getText().toString();

        EditText portET = findViewById(R.id.port);
        SwitchCompat multiChannelSwitch = findViewById(R.id.multiChannelSwitch);
        String portString = portET.getText().toString();
        boolean multiChannel = multiChannelSwitch.isActivated();

        int port = Integer.parseInt(portString);

        HubConnectionManagerApplicationSide connectionManager = (HubConnectionManagerApplicationSide) this.getHubConnectionManager();
//        connectionManager.
        try {
            HubConnectorDescription hcd = new TCPHubConnectorDescriptionImpl(hostName, port, multiChannel);
            connectionManager.connectHub(hcd);
            Toast.makeText(this, "connected to hub", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SharkException e) {
            throw new RuntimeException(e);
        }

        /*

        SharkPeerBasic sharkPeerBasic = new SharkPeerBasicImpl(this.getASAPPeer());
//        this.getASAPAndroidPeer().sendASAPMessage("app/x-asapHubtest", "asap://app",
//                String.format("hello from android peer %s", this.getASAPPeer().getPeerID()).getBytes());
//        this.getASAPPeer().addASAPMessageReceivedListener("app/x-asapHubtest", this);
        try {
            sharkPeerBasic.addHubDescription(
                    new TCPHubConnectorDescriptionImpl(hostName, port, multiChannel));
        } catch (IOException e) {
            String s = "cannot create hub description: " + hostName + ":" + port;
            Log.w(this.getLogStart(), s);
            Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        }
         */

        if(on) {
            this.connectASAPHubs();

        } else {
            this.disconnectASAPHubs();
        }
    }

    public void onConnectHubButtonClick(View view) throws ASAPException {
        this.hubAction(true);
    }

    public void onDisconnectHubButtonClick(View view) throws ASAPException {
        this.hubAction(false);
    }
    public void onRefreshButtonClick(View view) throws ASAPException {
        HubConnectionManagerApplicationSide connectionManager = (HubConnectionManagerApplicationSide) this.getHubConnectionManager();
        connectionManager.refreshHubList();

    }

    private void doHandleReceivedMessages(ASAPMessages asapMessages) {
        Log.d("mytag", "going to handle received messages with uri: "
                + asapMessages.getURI());
    }

    @Override
    public void notifyHubListReceived() {
        List<String> connectedHubs = new ArrayList<>();
        for(HubConnectorDescription hcd:hubConnectionManager.getConnectedHubs()){
            try {
                connectedHubs.add(hcd.getHostName().toString());
            } catch (ASAPHubException e) {
                throw new RuntimeException(e);
            }
        }
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, connectedHubs.toArray(new String[0])));


    }
}
