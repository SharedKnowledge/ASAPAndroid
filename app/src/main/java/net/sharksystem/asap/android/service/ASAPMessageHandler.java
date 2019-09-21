package net.sharksystem.asap.android.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPOnlineMessageSender;
import net.sharksystem.asap.android.ASAP;
import net.sharksystem.asap.android.ASAPServiceMethods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ASAPMessageHandler extends Handler {
    private static final String LOGSTART = "ASAPMessageHandler";
    private ASAPService asapService;

    ASAPMessageHandler(ASAPService asapService) {
        this.asapService = asapService;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case ASAPServiceMethods.SEND_MESSAGE:
                    this.handleSendMessage(msg);
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

    private void handleSendMessage(Message msg) {
        Bundle msgData = msg.getData();
        if (msgData != null) {
            String format = msgData.getString(ASAP.FORMAT);
            String uri = msgData.getString(ASAP.URI);
            String recipient = msgData.getString(ASAP.RECIPIENT);
            byte[] content = msgData.getByteArray(ASAP.MESSAGE_CONTENT);
            int era = msgData.getInt(ASAP.ERA);

            Log.d(LOGSTART, "received send message request");
            if(uri == null) {
                Log.e(LOGSTART, "uri must not be empty");
                return;
            }

            if(content == null) {
                Log.e(LOGSTART, "format content must not be empty");
                return;
            }

            if(format == null) {
                Log.e(LOGSTART, "format must not be empty");
                return;
            }

            try {
                StringBuilder sb = new StringBuilder();
                sb.append("going to send message to service:");
                sb.append(" | format: ");
                sb.append(format);
                sb.append(" | uri: ");
                sb.append(uri);
                sb.append(" | recipient: ");
                if(recipient == null) {
                    sb.append("not set");
                } else {
                    sb.append(recipient);
                }
                sb.append(" | era: ");
                sb.append(era);

                Log.d(LOGSTART, sb.toString());

                ASAPOnlineMessageSender asapOnlineMessageSender =
                        this.asapService.getASAPOnlineMessageSender();

                if(asapOnlineMessageSender == null) {
                    Log.e(LOGSTART, "got no asap online message sender from service");
                    return;
                }

                asapOnlineMessageSender.sendASAPAssimilate(
                        format, uri, recipient, content, era);

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
