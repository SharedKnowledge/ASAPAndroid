package net.sharksystem.asap.android.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPActivity;
import net.sharksystem.hub.peerside.TCPHubConnectorDescriptionImpl;

import java.io.IOException;
import java.util.List;

public class ASAPExampleHubTesterActivity extends ASAPActivity {
    // TODO use this Activity as blueprint to refactor the ASAPExampleHubManagementActivity class
    private TextView messageTextView;
    private ASAPMessageReceivedListener receivedListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_hub_tester_layout);
        messageTextView = findViewById(R.id.textViewReceivedASAPMessages);

        this.receivedListener = new ASAPMessageReceivedListener() {
            @Override
            public void asapMessagesReceived(ASAPMessages asapMessages,
                                             String senderE2E, // E2E part
                                             List<ASAPHop> asapHops) {
                ASAPExampleHubTesterActivity.this.doHandleReceivedMessages(asapMessages);
            }
        };

        this.getASAPAndroidPeer().addASAPMessageReceivedListener(
                "myformat", // listen to this app
                this.receivedListener);
    }

    private void hubAction(boolean on) throws ASAPException {
        EditText hostnameET = findViewById(R.id.hostname);
        String hostName = hostnameET.getText().toString();

        EditText portET = findViewById(R.id.port);
        SwitchCompat multiChannelSwitch = findViewById(R.id.multiChannelSwitch);
        String portString = portET.getText().toString();
        boolean multiChannel = multiChannelSwitch.isActivated();

        int port = Integer.parseInt(portString);

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

            Toast.makeText(this, "connected to hub", Toast.LENGTH_SHORT).show();
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

    private void doHandleReceivedMessages(ASAPMessages asapMessages) {
        Log.d("mytag", "going to handle received messages with uri: "
                + asapMessages.getURI());
    }
}
