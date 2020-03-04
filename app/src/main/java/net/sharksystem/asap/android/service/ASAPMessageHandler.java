package net.sharksystem.asap.android.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.sharksystem.asap.ASAPEngine;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.MultiASAPEngineFS;
import net.sharksystem.asap.android.ASAPServiceMessage;
import net.sharksystem.asap.android.ASAPServiceMethods;

import java.io.IOException;
import java.util.Collection;

/**
 * Class handles messages on service side.
 */
class ASAPMessageHandler extends Handler {
    private static final String LOGSTART = "ASAPMessageHandler";
    private ASAPService asapService;

    ASAPMessageHandler(ASAPService asapService) {
        this.asapService = asapService;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.d(LOGSTART, "handleMessage called with what == " + msg.what);
        try {
            switch (msg.what) {
                case ASAPServiceMethods.SEND_MESSAGE:
                    Log.d(LOGSTART, "handleMessage SEND_MESSAGE called");
                    this.handleSendMessage(msg);
                    break;

                case ASAPServiceMethods.CREATE_CLOSED_CHANNEL:
                    Log.d(LOGSTART, "handleMessage CREATE_CLOSED_CHANNEL called");
                    this.handleCreateClosedChannel(msg);
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

                case ASAPServiceMethods.START_BROADCASTS:
                    this.asapService.resumeBroadcasts();
                    break;

                case ASAPServiceMethods.STOP_BROADCASTS:
                    this.asapService.pauseBroadcasts();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
//        catch(ASAPException e) {
        catch(Throwable e) {
            Log.d(LOGSTART, e.getLocalizedMessage());
        }
    }

    private void handleSendMessage(Message msg) throws ASAPException, IOException {
        ASAPServiceMessage asapMessage = ASAPServiceMessage.createASAPServiceMessage(msg);

        MultiASAPEngineFS multiASAPEngine = asapService.getMultiASAPEngine();
        ASAPEngine asapEngine = multiASAPEngine.getEngineByFormat(asapMessage.getFormat());

        Log.d(this.getLogStart(), "don't explicitely create open channel - just add message");
        // asapEngine.createChannel(asapMessage.getURI());

        asapEngine.add(asapMessage.getURI().toString(), asapMessage.getASAPMessage());
    }

    private void handleCreateClosedChannel(Message msg) throws ASAPException, IOException {
        ASAPServiceMessage asapMessage = ASAPServiceMessage.createASAPServiceMessage(msg);

        MultiASAPEngineFS multiASAPEngine = asapService.getMultiASAPEngine();
        ASAPEngine asapEngine = multiASAPEngine.getEngineByFormat(asapMessage.getFormat());

        Collection<CharSequence> recipients = asapMessage.getRecipients();

        Log.d(this.getLogStart(), "create closed channel: "
            + asapMessage.getURI() + " | " + asapMessage.getRecipients());
        asapEngine.createChannel(asapMessage.getURI(), recipients);
    }

    private String getLogStart() {
        return net.sharksystem.asap.util.Log.startLog(this).toString();
    }
}
