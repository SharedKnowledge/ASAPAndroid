package net.sharksystem.asap.android.example;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import net.sharksystem.asap.ASAPConnectionHandler;
import net.sharksystem.asap.ASAPEncounterManagerImpl;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPActivity;

/**
 * This class is an example activity to demonstrate how to establish a TCP connection with
 * another ASAPPeer which provides a TCP Server-Socket.
 * This is useful is the other ASAPPeer is in the same network and reachable via TCP.
 * This could be the case if the device running the other ASAPPeer is in the same network (connected to the same WiFi-AP or WiFi-Direct-Group).
 */
public class ASAPExampleTCPEncounterActivity extends ASAPActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_tcp_encounter_layout);
        Button connectButton = findViewById(R.id.connectButtonTCPEncounter);
        connectButton.setOnClickListener(this);
    }

    /**
     * This method is called when the connect button is clicked.
     * It reads the hostname and port from the EditText fields and starts the TCP encounter.
     * @param view the view that was clicked
     */
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.connectButtonTCPEncounter) {
            EditText hostnameET = findViewById(R.id.hostnameTCPEncounter);
            String hostName = hostnameET.getText().toString();
            EditText portET = findViewById(R.id.portTCPEncounter);
            String portString = portET.getText().toString();
            int port = Integer.parseInt(portString);
            this.startTCPEncounter(hostName, port);
        }
    }
}
