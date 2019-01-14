package net.sharksystem.aasp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service that searches and creates wifi p2p connections
 * to run an AASP session.
 */

public class WifiAASPService extends Service {
    public WifiAASPService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
