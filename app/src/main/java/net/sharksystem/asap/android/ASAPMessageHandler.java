package net.sharksystem.asap.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.sharksystem.asap.ASAPEngine;
import net.sharksystem.asap.ASAPException;

import java.io.IOException;

class ASAPMessageHandler extends Handler {
    private static final String LOGSTART = "AASPMessageHandler";
    private ASAPService ASAPService;

    ASAPMessageHandler(ASAPService context) {
        this.ASAPService = context;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case ASAPServiceMethods.ADD_MESSAGE:
                    Bundle msgData = msg.getData();
                    if (msgData != null) {
                        String uri = msgData.getString(ASAP.URI);
                        String content = msgData.getString(ASAP.MESSAGE_CONTENT);

                        String text = uri + " / " + content;
                        Log.d(LOGSTART, text);

                        try {
                            ASAPEngine aaspEngine = this.ASAPService.getAASPEngine();
                            if (aaspEngine == null) {
                                Log.d(LOGSTART, "NO AASPEngine!!");
                            } else {
                                aaspEngine.add(uri, content);
                                Log.d(LOGSTART, "wrote");

                                // simulate broadcast
                                Intent intent = new ASAPBroadcastIntent(
                                        ASAP.UNKNOWN_USER,
                                        this.ASAPService.getAASPRootFolderName(),
                                        uri,
                                        aaspEngine.getEra());

                                this.ASAPService.sendBroadcast(intent);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d(LOGSTART, "finish aasp write");
                    break;

                case ASAPServiceMethods.START_WIFI_DIRECT:
                    this.ASAPService.startWifiDirect();
                    break;

                case ASAPServiceMethods.STOP_WIFI_DIRECT:
                    this.ASAPService.stopWifiDirect();
                    break;

                case ASAPServiceMethods.START_BLUETOOTH:
                    this.ASAPService.startBluetooth();
                    break;

                case ASAPServiceMethods.STOP_BLUETOOTH:
                    this.ASAPService.stopBluetooth();
                    break;

                case ASAPServiceMethods.START_BROADCASTS:
                    this.ASAPService.resumeBroadcasts();
                    break;

                case ASAPServiceMethods.STOP_BROADCASTS:
                    this.ASAPService.pauseBroadcasts();
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
