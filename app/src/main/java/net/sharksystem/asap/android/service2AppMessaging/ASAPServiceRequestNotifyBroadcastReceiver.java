package net.sharksystem.asap.android.service2AppMessaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.bluetooth.BluetoothEngine;
import net.sharksystem.hub.peerside.TCPHubConnectorDescriptionImpl;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.util.Set;

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
                this.notificationListener.asapNotifyBTDiscoveryStopped();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERY_STARTED:
                Log.d(this.getLogStart(), "notified bluetooth discovery started");
                this.notificationListener.asapNotifyBTDiscoveryStarted();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_DISCOVERABLE_STARTED:
                Log.d(this.getLogStart(), "notified bluetooth discoverable started");
                this.notificationListener.asapNotifyBTDiscoverableStarted();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_ENVIRONMENT_STARTED:
                Log.d(this.getLogStart(), "notified bluetooth environment started");
                this.notificationListener.asapNotifyBTEnvironmentStarted();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_BT_ENVIRONMENT_STOPPED:
                Log.d(this.getLogStart(), "notified bluetooth environment stopped");
                this.notificationListener.asapNotifyBTEnvironmentStopped();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_ONLINE_PEERS_CHANGED:
                Log.d(this.getLogStart(), "notified online peers changed");
                String peers = intent.getStringExtra(ASAPServiceRequestNotifyIntent.ASAP_PARAMETER_1);
                Log.d(this.getLogStart(), "new peers: " + peers);
                Set<CharSequence> peersSet = SerializationHelper.string2CharSequenceSet(peers);
                this.notificationListener.asapNotifyOnlinePeersChanged(peersSet);
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_HUBS_CONNECTED:
                Log.d(this.getLogStart(), "notified hubs connected");
                this.notificationListener.asapNotifyHubsConnected();
                break;

            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_HUBS_DISCONNECTED:
                Log.d(this.getLogStart(), "notified hubs disconnected");
                this.notificationListener.asapNotifyHubsDisconnected();
                break;
            case ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_HUB_LIST_AVAILABLE:
                Log.d(this.getLogStart(), "notified received current hub list");
                byte[] serializedHubDescriptions = intent.getByteArrayExtra(ASAPServiceRequestNotifyIntent.ASAP_PARAMETER_1);
                try {
                    this.notificationListener.asapNotifyHubListAvailable(TCPHubConnectorDescriptionImpl.
                            deserializeHubConnectorDescriptionList(serializedHubDescriptions));
                } catch (IOException |ASAPException e) {
                    Log.e(this.getLogStart(),
                            String.format("error while deserialization of HubDescriptorList: %s",
                                    e.getLocalizedMessage()));
                }
                break;

            default:
                Log.d(this.getLogStart(), "unknown request / notification number: " + cmd);
        }
    }

    private String getLogStart() {
        return "ASAPServiceRequestNotifyBCReceiver";
    }
}
