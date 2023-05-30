package net.sharksystem.asap.android.example;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.widget.SwitchCompat;

import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPActivity;
import net.sharksystem.asap.android.apps.HubConnectionManagerApplicationSide;
import net.sharksystem.asap.android.apps.HubManagerStatusChangedListener;
import net.sharksystem.hub.HubConnectionManager;
import net.sharksystem.hub.peerside.HubConnectorDescription;
import net.sharksystem.hub.peerside.TCPHubConnectorDescriptionImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ASAPExampleHubTesterActivity extends ASAPActivity implements HubManagerStatusChangedListener{
    private HubConnectionManagerApplicationSide hubConnectionManager;
    private ListView listViewConnectedHubs;
    private ListView listViewFailedAttempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_hub_tester_layout);

        listViewConnectedHubs = findViewById(R.id.listViewAvailableHubs);
        listViewFailedAttempts = findViewById(R.id.listViewFailedAttempts);

        hubConnectionManager = (HubConnectionManagerApplicationSide) this.getHubConnectionManager();
        hubConnectionManager.addListener(this);
    }

    private void hubAction(boolean connect) {
        EditText hostnameET = findViewById(R.id.hostname);
        String hostName = hostnameET.getText().toString();
        EditText portET = findViewById(R.id.port);
        SwitchCompat multiChannelSwitch = findViewById(R.id.multiChannelSwitch);
        String portString = portET.getText().toString();
        boolean multiChannel = multiChannelSwitch.isActivated();
        int port = Integer.parseInt(portString);

        try {
            HubConnectorDescription hcd = new TCPHubConnectorDescriptionImpl(hostName, port, multiChannel);
            if (connect) {
                this.connectASAPHubs(hcd);
            } else {
                this.disconnectASAPHubs(hcd);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        hubConnectionManager.refreshHubList();
    }

    public void onConnectHubButtonClick(View view) {
        this.hubAction(true);
    }

    public void onDisconnectHubButtonClick(View view) {
        this.hubAction(false);
    }

    public void onRefreshButtonClick(View view) {
        HubConnectionManagerApplicationSide connectionManager = (HubConnectionManagerApplicationSide) this.getHubConnectionManager();
        connectionManager.refreshHubList();
    }

    @Override
    public void notifyHubListReceived() {
        List<String> connectedHubs = new ArrayList<>();
        for (HubConnectorDescription hcd : hubConnectionManager.getConnectedHubs()) {
            connectedHubs.add(hcd.toString());
        }
        List<String> failedAttempts = new ArrayList<>();
        for (HubConnectionManager.FailedConnectionAttempt attempt : hubConnectionManager.getFailedConnectionAttempts()) {
            failedAttempts.add(attempt.getHubConnectorDescription().toString());
        }
        listViewConnectedHubs.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, connectedHubs.toArray(new String[0])));
        listViewFailedAttempts.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, failedAttempts.toArray(new String[0])));
    }

}
