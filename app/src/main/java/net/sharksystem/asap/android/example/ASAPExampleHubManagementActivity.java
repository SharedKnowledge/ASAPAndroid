package net.sharksystem.asap.android.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import net.sharksystem.SharkPeerHubSupport;
import net.sharksystem.SharkPeerHubSupportImpl;
import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPActivity;
import net.sharksystem.hub.peerside.TCPHubConnectorDescription;

import java.io.IOException;

public class ASAPExampleHubManagementActivity extends ASAPActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_hub_management_layout);
    }

    private void hubAction(boolean on) {
        EditText hostnameET = findViewById(R.id.hostname);
        String hostName = hostnameET.getText().toString();

        EditText portET = findViewById(R.id.port);
        String portString = portET.getText().toString();

        int port = Integer.parseInt(portString);

        SharkPeerHubSupport sharkPeerSettings = new SharkPeerHubSupportImpl(this.getASAPPeer());
        try {
            sharkPeerSettings.addHubDescription(new TCPHubConnectorDescription(hostName, port));
        } catch (IOException e) {
            String s = "cannot create hub description: " + hostName + ":" + port;
            Log.w(this.getLogStart(), s);
            Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        }

        if(on) {
            this.connectASAPHubs();
        } else {
            this.disconnectASAPHubs();
        }
    }

    public void onConnectHubButtonClick(View view) {
        this.hubAction(true);
    }

    public void onDisconnectHubButtonClick(View view) {
        this.hubAction(false);
    }
}
