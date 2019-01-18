package net.sharksystem.aasp.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Messenger;
import android.widget.Toast;

/**
 * Service that searches and creates wifi p2p connections
 * to run an AASP session.
 */

public class AASPService extends Service {
    public AASPService() {
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger mMessenger;

    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();

        // create handler
        this.mMessenger = new Messenger(new AASPMessageHandler(this));

        // return binder interface
        return mMessenger.getBinder();
    }
}
