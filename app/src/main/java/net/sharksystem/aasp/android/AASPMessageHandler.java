package net.sharksystem.aasp.android;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

class AASPMessageHandler extends Handler {
    private Context applicationContext;

    AASPMessageHandler( Context context) {
        this.applicationContext = context;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case AASP.WRITE_MESSAGE:
                Toast.makeText(applicationContext, "write Message!", Toast.LENGTH_SHORT).show();
                break;
            case AASP.NEW_ERA:
                Toast.makeText(applicationContext, "new era!", Toast.LENGTH_SHORT).show();
                break;
            default:
                super.handleMessage(msg);
        }

        // simulate broadcast
        Intent intent = new Intent();
        intent.setAction(AASP.BROADCAST_ACTION);
        intent.putExtra(AASP.FOLDER,"sampleFolder");
        this.applicationContext.sendBroadcast(intent);
    }

}
