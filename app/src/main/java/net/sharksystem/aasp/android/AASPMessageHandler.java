package net.sharksystem.aasp.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import net.sharksystem.asp3.ASP3Engine;

import java.io.IOException;

class AASPMessageHandler extends Handler {
    private AASPService aaspService;

    AASPMessageHandler(AASPService context) {
        this.aaspService = context;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case AASPServiceMethods.ADD_MESSAGE:
                Bundle msgData = msg.getData();
                if (msgData != null) {
                    String uri = msgData.getString(AASP.URI);
                    String content = msgData.getString(AASP.MESSAGE_CONTENT);

                    String text = uri + " / " + content;
                    Toast.makeText(aaspService, text, Toast.LENGTH_SHORT).show();

                    try {
                        ASP3Engine aaspEngine = this.aaspService.getAASPEngine();
                        if(aaspEngine == null) {
                            Toast.makeText(aaspService, "NO AASPEngine!!", Toast.LENGTH_SHORT).show();
                        } else {
                            aaspEngine.add(uri, content);
                            Toast.makeText(aaspService, "wrote", Toast.LENGTH_SHORT).show();

                            // simulate broadcast
                            Intent intent = new Intent();
                            intent.setAction(AASP.BROADCAST_ACTION);
                            intent.putExtra(AASP.FOLDER,this.aaspService.getAASPRootFolderName());
                            intent.putExtra(AASP.URI,uri);
                            intent.putExtra(AASP.ERA,aaspEngine.getEra());
                            this.aaspService.sendBroadcast(intent);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Toast.makeText(aaspService, "finish aasp write", Toast.LENGTH_SHORT).show();
                break;

            case AASPServiceMethods.START_WIFI_DIRECT:
                this.aaspService.startWifiDirect();
                break;

            case AASPServiceMethods.STOP_WIFI_DIRECT:
                this.aaspService.stopWifiDirect();
                break;

            default:
                super.handleMessage(msg);
        }
    }

}
