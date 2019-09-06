package net.sharksystem.asap.android.example;

import android.content.IntentFilter;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import net.sharksystem.asap.android.ASAP;
import net.sharksystem.asap.android.apps.ASAPActivity;
import net.sharksystem.asap.android.ASAPServiceMethods;
import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPApplication;

public class MainActivity extends ASAPActivity {
    private static final CharSequence TESTURI ="asap://testuri";
    private static final CharSequence TESTMESSAGE = "Hi there from asap writing activity";

    public MainActivity() {
        super(ASAPApplication.getASAPApplication());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create broadcast receiver
        ExampleASAPBroadcastReceiver br = new ExampleASAPBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ASAP.BROADCAST_ACTION);
        this.registerReceiver(br, filter);
    }

    public void onClick(View view) {
        View writeButton = findViewById(R.id.writeButton);
        View startWifiButton = findViewById(R.id.startWifiDirect);
        View stopWifiButton = findViewById(R.id.stopWifiDirect);
        View startBTButton = findViewById(R.id.startBT);
        View stopBTButton = findViewById(R.id.stopBT);

        if(view == writeButton) {
            // Create and send a message to the service, using a supported 'what' value
            Message msg = Message.obtain(null, ASAPServiceMethods.ADD_MESSAGE, 0, 0);
            Bundle msgData = new Bundle();
            msgData.putCharSequence(ASAP.URI, TESTURI);
            msgData.putCharSequence(ASAP.MESSAGE_CONTENT, TESTMESSAGE);
            msg.setData(msgData);

            this.sendMessage2Service(msg);
        }
        else if(view == startWifiButton) {
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
    }

    @Override
    public void asapNotifyBTDiscoverableStopped() {
        super.asapNotifyBTDiscoverableStopped();
        Log.d(this.getLogStart(), "got notified: discoverable stopped");
        Toast.makeText(this, "discoverable stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void aspNotifyBTDiscoveryStopped() {
        super.aspNotifyBTDiscoveryStopped();
        Log.d(this.getLogStart(), "got notified: discovery stopped");
        Toast.makeText(this, "discovery stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void aspNotifyBTDiscoveryStarted() {
        super.aspNotifyBTDiscoveryStarted();
        Log.d(this.getLogStart(), "got notified: discovery started");
        Toast.makeText(this, "discovery started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void aspNotifyBTDiscoverableStarted() {
        super.aspNotifyBTDiscoverableStarted();
        Log.d(this.getLogStart(), "got notified: discoverable started");
        Toast.makeText(this, "discoverable started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void aspNotifyBTEnvironmentStarted() {
        super.aspNotifyBTEnvironmentStarted();
        Log.d(this.getLogStart(), "got notified: bluetooth on");
        Toast.makeText(this, "bluetooth on", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void aspNotifyBTEnvironmentStopped() {
        super.aspNotifyBTEnvironmentStopped();
        Log.d(this.getLogStart(), "got notified: bluetooth off");
        Toast.makeText(this, "bluetooth off", Toast.LENGTH_SHORT).show();
    }
}