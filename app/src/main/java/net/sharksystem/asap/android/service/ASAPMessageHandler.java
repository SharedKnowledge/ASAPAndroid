package net.sharksystem.asap.android.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.asap.android.ASAPServiceMessage;
import net.sharksystem.asap.android.ASAPServiceMethods;

import java.io.IOException;

/**
 * Class handles messages on service side.
 */
class ASAPMessageHandler extends Handler {
    private ASAPService asapService;

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

                default:
                    super.handleMessage(msg);
            }
        }
//        catch(ASAPException e) {
        catch(Throwable e) {
            Log.d(this.getLogStart(), e.getLocalizedMessage());
        }
    }

    private void handleSendMessage(Message msg) throws ASAPException, IOException {
        ASAPServiceMessage asapMessage = ASAPServiceMessage.createASAPServiceMessage(msg);

        Log.d(this.getLogStart(), "service will send: "+ asapMessage);

        ASAPPeerFS asapPeer = this.asapService.getASAPPeer();

        asapPeer.sendASAPMessage(
                asapMessage.getFormat(),
                asapMessage.getURI(),
                asapMessage.getASAPMessage()
        );

        Log.d(this.getLogStart(), "done sending");
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
        return net.sharksystem.asap.util.Log.startLog(this).toString();
    }
}
