package net.sharksystem.asap.android.service;

import static net.sharksystem.asap.android.ASAPServiceMessage.HUB_CONNECTOR_DESCRIPTION_TAG;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.asap.android.ASAPServiceMessage;
import net.sharksystem.asap.android.ASAPServiceMethods;
import net.sharksystem.hub.peerside.AbstractHubConnectorDescription;
import net.sharksystem.hub.peerside.HubConnectorDescription;

import java.io.IOException;

/**
 * Class handles messages on service side.
 */
class ASAPMessageHandler extends Handler {
    private ASAPService asapService;

    public static final boolean DEFAULT_MULTICHANNEL = true;

    ASAPMessageHandler(ASAPService asapService) {
        this.asapService = asapService;
    }

    @Override
    public void handleMessage(Message msg) {
        //Log.d(this.getLogStart(), "handleMessage called with what == " + msg.what);
        try {
            switch (msg.what) {
                case ASAPServiceMethods.SEND_MESSAGE:
                    Log.d(this.getLogStart(), "handleMessage SEND_MESSAGE called");
                    this.handleSendMessage(msg);
                    break;

                case ASAPServiceMethods.CREATE_CLOSED_CHANNEL:
                    Log.d(this.getLogStart(), "handleMessage CREATE_CLOSED_CHANNEL called");
                    this.handleCreateClosedChannel(msg);
                    break;

                case ASAPServiceMethods.START_BROADCASTS:
                    this.asapService.resumeBroadcasts();
                    break;

                case ASAPServiceMethods.STOP_BROADCASTS:
                    this.asapService.pauseBroadcasts();
                    break;

                case ASAPServiceMethods.START_WIFI_DIRECT:
                    this.asapService.startWifiDirect();
                    break;

                case ASAPServiceMethods.ASK_PROTOCOL_STATUS:
                    this.asapService.propagateProtocolStatus();
                    break;

                case ASAPServiceMethods.STOP_WIFI_DIRECT:
                    this.asapService.stopWifiDirect();
                    break;

                case ASAPServiceMethods.START_BLUETOOTH:
                    this.asapService.startBluetooth();
                    break;

                case ASAPServiceMethods.STOP_BLUETOOTH:
                    this.asapService.stopBluetooth();
                    break;

                case ASAPServiceMethods.START_BLUETOOTH_DISCOVERABLE:
                    this.asapService.startBluetoothDiscoverable();
                    break;

                case ASAPServiceMethods.START_BLUETOOTH_DISCOVERY:
                    this.asapService.startBluetoothDiscovery();
                    break;

                case ASAPServiceMethods.START_RECONNECT_PAIRED_DEVICES:
                    this.asapService.startReconnectPairedDevices();
                    break;

                case ASAPServiceMethods.STOP_RECONNECT_PAIRED_DEVICES:
                    this.asapService.startReconnectPairedDevices();
                    break;

                case ASAPServiceMethods.START_LORA:
                    this.asapService.startLoRa();
                    break;

                case ASAPServiceMethods.STOP_LORA:
                    this.asapService.stopLoRa();
                    break;

                // // HUB_CONNECTION_CHANGED
                // (byte[] serializedHubConnectorDescription, boolean connect/disconnect);
                case ASAPServiceMethods.HUB_CONNECTION_CHANGED:
                    Log.d(this.getLogStart(), "handleMessage HUB_CONNECTION_CHANGED called");
                    byte[] serializedHcd =
                        msg.getData().getByteArray(ASAPServiceMessage.HUB_CONNECTOR_DESCRIPTION_TAG);

                    HubConnectorDescription hcd = null;
                    if(serializedHcd != null) {
                        // deserialize hub connector description
                        hcd = AbstractHubConnectorDescription.
                            createHubConnectorDescription(serializedHcd);
                    }
                    // connect or disconnect
                    boolean connect = msg.getData().getBoolean(ASAPServiceMethods.BOOLEAN_PARAMETER);

                    // call hub manager to handle request
                    this.asapService.getHubConnectionManager().connectionChanged(hcd, connect);
                    break;

                // ASK_HUB_CONNECTIONS; no parameters.
                case ASAPServiceMethods.ASK_HUB_CONNECTIONS:
                    this.asapService.getHubConnectionManager().refreshHubList();

                default:
                    super.handleMessage(msg);
            }
        }
//        catch(ASAPException e) {
        catch (Throwable e) {
            Log.d(this.getLogStart(), e.getLocalizedMessage());
        }
    }


    private byte[] getHubDescription(Message msg) throws ASAPException {
        Bundle msgData = msg.getData();
        if(msgData ==null) {
            Log.e(this.getLogStart(), "send message must contain parameters");
            throw new ASAPException("send message must contain parameters");
        }
        byte[] hubConnectorDescription = msgData.getByteArray(HUB_CONNECTOR_DESCRIPTION_TAG);
        if(hubConnectorDescription == null) throw new ASAPException("hub connector data not set");

        return hubConnectorDescription;
    }

    private void handleSendMessage(Message msg) throws ASAPException, IOException {
        ASAPServiceMessage asapMessage = ASAPServiceMessage.createASAPServiceMessage(msg);

        Log.d(this.getLogStart(), "service will send: "+ asapMessage);

        ASAPPeerFS asapPeer = this.asapService.getASAPPeer();

        if(asapMessage.getPersistent()) {
            asapPeer.sendASAPMessage(
                    asapMessage.getFormat(),
                    asapMessage.getURI(),
                    asapMessage.getASAPMessage()
            );
            Log.d(this.getLogStart(), "sent message (and stored for re-delivery)");
        } else {
            asapPeer.sendTransientASAPMessage(
                    asapMessage.getFormat(),
                    asapMessage.getURI(),
                    asapMessage.getASAPMessage());
            Log.d(this.getLogStart(), "sent message online only");
        }
    }

    private void handleCreateClosedChannel(Message msg) throws ASAPException, IOException {
        ASAPServiceMessage asapMessage = ASAPServiceMessage.createASAPServiceMessage(msg);
        Log.e(this.getLogStart(), "closed channels are currently not supported");
        throw new ASAPException("closed channels are currently not supported");
/*
        ASAPPeer asapPeer = asapService.getASAPPeer();
        ASAPEngine asapEngine = asapPeer.getEngineByFormat(asapMessage.getFormat());

        Collection<CharSequence> recipients = asapMessage.getRecipients();

        Log.d(this.getLogStart(), "create closed channel: "
            + asapMessage.getURI() + " | " + asapMessage.getRecipients());
        asapEngine.createChannel(asapMessage.getURI(), recipients);
 */
    }

    private String getLogStart() {
        return net.sharksystem.utils.Log.startLog(this).toString();
    }
}
