package net.sharksystem.asap.android.service2AppMessaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.sharksystem.asap.android.bluetooth.BluetoothEngine;

public class ASAPServiceRequestNotifyBroadcastReceiver extends BroadcastReceiver {
    private final ASAPServiceRequestListener requestListener;
    private final ASAPServiceNotificationListener notificationListener;

    public ASAPServiceRequestNotifyBroadcastReceiver(
            ASAPServiceRequestListener requestListener,
            ASAPServiceNotificationListener notificationListener) {

        this.requestListener = requestListener;
        this.notificationListener = notificationListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int cmd = intent.getIntExtra(
                ASAPServiceRequestNotifyIntent.ASAP_REQUEST_COMMAND, -1);

        switch(cmd) {
            //////////////////////////////////////////////////////////////////////////
            //                               requests                               //
            //////////////////////////////////////////////////////////////////////////

            case ASAPServiceRequestNotifyIntent.ASAP_RQ_ASK_USER_TO_ENABLE_BLUETOOTH:
                Log.d(this.getLogStart(), "was asked to enable bluetooth");
                this.requestListener.asapSrcRq_enableBluetooth();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_RQ_ASK_USER_TO_START_BT_DISCOVERABLE:
                Log.d(this.getLogStart(), "was asked to start bluetooth discoverable");

                int time = intent.getIntExtra(ASAPServiceRequestNotifyIntent.ASAP_PARAMETER_1,
                        BluetoothEngine.DEFAULT_VISIBILITY_TIME);

                this.requestListener.asapSrcRq_startBTDiscoverable(time);
                break;

                //////////////////////////////////////////////////////////////////////////
                //                               notifications                          //
                //////////////////////////////////////////////////////////////////////////

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERABLE_STOPPED:
                Log.d(this.getLogStart(), "notified bluetooth discoverable stopped");
                this.notificationListener.asapNotifyBTDiscoverableStopped();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERY_STOPPED:
                Log.d(this.getLogStart(), "notified bluetooth discovery stopped");
                this.notificationListener.aspNotifyBTDiscoveryStopped();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERY_STARTED:
                Log.d(this.getLogStart(), "notified bluetooth discovery started");
                this.notificationListener.aspNotifyBTDiscoveryStarted();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERABLE_STARTED:
                Log.d(this.getLogStart(), "notified bluetooth discoverable started");
                this.notificationListener.aspNotifyBTDiscoverableStarted();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_ENVIRONMENT_STARTED:
                Log.d(this.getLogStart(), "notified bluetooth environment started");
                this.notificationListener.aspNotifyBTEnvironmentStarted();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_ENVIRONMENT_STOPPED:
                Log.d(this.getLogStart(), "notified bluetooth environment stopped");
                this.notificationListener.aspNotifyBTEnvironmentStopped();
                break;

            default:
                Log.d(this.getLogStart(), "unknown request / notification number: " + cmd);

        }
    }

    private String getLogStart() {
        return "ASAPServiceRequestNotifyBCReceiver";
    }
}
