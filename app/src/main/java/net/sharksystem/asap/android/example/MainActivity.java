package net.sharksystem.asap.android.example;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import net.sharksystem.asap.android.ASAP;
import net.sharksystem.asap.android.ASAPServiceMethods;
import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.util.ASAPActivityHelper;
import net.sharksystem.asap.android.util.ASAPServiceNotificationListener;

public class MainActivity extends AppCompatActivity implements ASAPServiceNotificationListener {
    private static final CharSequence TESTURI ="asap://testuri";
    private static final CharSequence TESTMESSAGE = "Hi there from asap writing activity";

    private ASAPActivityHelper asapActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create broadcast receiver
        ExampleASAPBroadcastReceiver br = new ExampleASAPBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ASAP.BROADCAST_ACTION);
        this.registerReceiver(br, filter);

        // create service request receiver
        this.asapActivityHelper = new ASAPActivityHelper(
                this, this,"alice");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.asapActivityHelper.onActivityResult(requestCode, resultCode, data);
    }

    protected void onDestroy() {
        super.onDestroy();
        this.asapActivityHelper.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.asapActivityHelper.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.asapActivityHelper.onStop();
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

            this.asapActivityHelper.sendMessage2Service(msg);
        }
        else if(view == startWifiButton) {
            Log.d(this.getLogStart(), "start wifi button pressed - send message");
            this.asapActivityHelper.sendMessage2Service(ASAPServiceMethods.START_WIFI_DIRECT);
        }
        else if(view == stopWifiButton) {
            Log.d(this.getLogStart(), "stop wifi button pressed - send message");
            this.asapActivityHelper.sendMessage2Service(ASAPServiceMethods.STOP_WIFI_DIRECT);
        }
        else if(view == startBTButton) {
            Log.d(this.getLogStart(), "start bt button pressed - ask service to start bt");
            this.asapActivityHelper.sendMessage2Service(ASAPServiceMethods.START_BLUETOOTH);
        }
        else if(view == stopBTButton) {
            Log.d(this.getLogStart(), "stop bt button pressed - send message");
            this.asapActivityHelper.sendMessage2Service(ASAPServiceMethods.STOP_BLUETOOTH);
        }
    }

    private String getLogStart() {
        return "ASAPExampleActivity";
    }

    @Override
    public void asapNotifyBTDiscoverableStopped() {
        Toast.makeText(this, "discoverable stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void aspNotifyBTDiscoveryStopped() {
        Toast.makeText(this, "discoverable stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void aspNotifyBTDiscoveryStarted() {
        Toast.makeText(this, "discovery started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void aspNotifyBTDiscoverableStarted() {
        Toast.makeText(this, "discoverable started", Toast.LENGTH_SHORT).show();
    }
}