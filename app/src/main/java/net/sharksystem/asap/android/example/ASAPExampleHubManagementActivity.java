package net.sharksystem.asap.android.example;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPActivity;

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

        if(on) {
            this.connectTCPHub(hostName, port);
        } else {
            this.disconnectTCPHub(hostName, port);
        }
    }

    public void onConnectHubButtonClick(View view) {
        this.hubAction(true);
    }

    public void onDisconnectHubButtonClick(View view) {
        this.hubAction(false);
    }
}
