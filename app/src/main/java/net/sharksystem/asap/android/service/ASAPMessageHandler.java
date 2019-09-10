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

class ASAPMessageHandler extends Handler {
    private static final String LOGSTART = "AASPMessageHandler";
    private ASAPService asapService;

    ASAPMessageHandler(ASAPService context) {
        this.asapService = context;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case ASAPServiceMethods.ADD_MESSAGE:
                    Bundle msgData = msg.getData();
                    if (msgData != null) {
                        String uri = msgData.getString(ASAP.URI);
                        String format = msgData.getString(ASAP.FORMAT);
                        byte[] content = msgData.getByteArray(ASAP.MESSAGE_CONTENT);

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

                        String text = uri + " / " + content;
                        Log.d(LOGSTART, text);

                        try {
                            ASAPOnlineMessageSender onlineMessageSender =
                                    this.asapService.getASAPOnlineMessageSender();

                            if(onlineMessageSender == null) {
                                Log.w(LOGSTART, "no online message sender in place");
                            } else {
                                Log.d(LOGSTART, "put message on online message handler");
                                onlineMessageSender.sendASAPAssimilate(format, uri, content);
                            }

                            /*
                            MultiASAPEngineFS asapMulti = this.asapService.getASAPEngine();
                            ASAPEngine asapEngine = asapMulti.getEngineByFormat(format);

                            if (asapEngine == null) {
                                Log.d(LOGSTART, "NO AASPEngine!!");
                            } else {
                                asapEngine.add(uri, content);
                                Log.d(LOGSTART, "wrote");

                                // simulate broadcast
                                Intent intent = new ASAPBroadcastIntent(
                                        ASAP.UNKNOWN_USER,
                                        this.asapService.getASAPRootFolderName(),
                                        uri,
                                        asapEngine.getEra());

                                this.asapService.sendBroadcast(intent);
                            }
                             */
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d(LOGSTART, "finish aasp write");
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
        catch(ASAPException e) {
            Log.d(LOGSTART, e.getLocalizedMessage());
        }
    }

}
