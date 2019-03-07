package net.sharksystem.aasp.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.sharksystem.aasp.AASPEngine;
import net.sharksystem.aasp.AASPException;

import java.io.IOException;

class AASPMessageHandler extends Handler {
    private static final String LOGSTART = "AASPMessageHandler";
    private AASPService aaspService;

    AASPMessageHandler(AASPService context) {
        this.aaspService = context;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case AASPServiceMethods.ADD_MESSAGE:
                    Bundle msgData = msg.getData();
                    if (msgData != null) {
                        String uri = msgData.getString(AASP.URI);
                        String content = msgData.getString(AASP.MESSAGE_CONTENT);

                        String text = uri + " / " + content;
                        Log.d(LOGSTART, text);

                        try {
                            AASPEngine aaspEngine = this.aaspService.getAASPEngine();
                            if (aaspEngine == null) {
                                Log.d(LOGSTART, "NO AASPEngine!!");
                            } else {
                                aaspEngine.add(uri, content);
                                Log.d(LOGSTART, "wrote");

                                // simulate broadcast
                                Intent intent = new AASPBroadcastIntent(
                                        AASP.UNKNOWN_USER,
                                        this.aaspService.getAASPRootFolderName(),
                                        uri,
                                        aaspEngine.getEra());

                                this.aaspService.sendBroadcast(intent);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d(LOGSTART, "finish aasp write");
                    break;

                case AASPServiceMethods.START_WIFI_DIRECT:
                    this.aaspService.startWifiDirect();
                    break;

                case AASPServiceMethods.STOP_WIFI_DIRECT:
                    this.aaspService.stopWifiDirect();
                    break;

                case AASPServiceMethods.START_BLUETOOTH:
                    this.aaspService.startBluetooth();
                    break;

                case AASPServiceMethods.STOP_BLUETOOTH:
                    this.aaspService.stopBluetooth();
                    break;

                case AASPServiceMethods.START_BROADCASTS:
                    this.aaspService.resumeBroadcasts();
                    break;

                case AASPServiceMethods.STOP_BROADCASTS:
                    this.aaspService.pauseBroadcasts();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
        catch(AASPException e) {
            Log.d(LOGSTART, e.getLocalizedMessage());
        }
    }

}
