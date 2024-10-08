package net.sharksystem.asap.android.example;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPActivity;
import net.sharksystem.asap.android.tcpEncounter.TCPEncounterListener;
import net.sharksystem.asap.android.tcpEncounter.TCPEncounterManagerApplicationSide;

/**
 * This class is an example activity to demonstrate how to establish a TCP connection with
 * another ASAPPeer which provides a TCP Server-Socket.
 * This is useful is the other ASAPPeer is in the same network and reachable via TCP.
 * This could be the case if the device running the other ASAPPeer is in the same network (connected to the same WiFi-AP or WiFi-Direct-Group).
 */
public class ASAPExampleTCPEncounterActivity extends ASAPActivity implements View.OnClickListener, TCPEncounterListener {

    private TCPEncounterManagerApplicationSide tcpEncounterManager;
    private TextView tcpEncounterStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_tcp_encounter_layout);

        tcpEncounterStatusText = findViewById(R.id.textViewTCPEncounterStatus);
        tcpEncounterStatusText.setText("status: disconnected");

        Button connectButton = findViewById(R.id.connectButtonTCPEncounter);
        connectButton.setOnClickListener(this);

        tcpEncounterManager = this.getTCPEncounterManager();
        tcpEncounterManager.setTcpEncounterListener(this);
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
            tcpEncounterManager.startTCPEncounter(hostName, port);
        }
    }

    @Override
    public void onEncounterSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tcpEncounterStatusText.setText("status: connected");
            }
        });
    }
}
