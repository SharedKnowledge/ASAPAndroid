package net.sharksystem.asap.android.apps;

import android.util.Log;

import net.sharksystem.asap.android.app2serviceMessaging.MessageFactory;
import net.sharksystem.hub.BasicHubConnectionManager;
import net.sharksystem.hub.HubConnectionManager;
import net.sharksystem.hub.peerside.HubConnectorDescription;

import java.io.IOException;

public class HubConnectionManagerApplicationSide
        extends BasicHubConnectionManager
        implements HubConnectionManager {

    private final ASAPActivity asapActivity;

    public HubConnectionManagerApplicationSide(ASAPActivity asapActivity) {
        this.asapActivity = asapActivity;
    }

    public void connectionChanged(
            HubConnectorDescription hubConnectorDescription, boolean connect) {
        // send message to hub manager on service side
        try {
            this.asapActivity.sendMessage2Service(
                    MessageFactory.createHubConnectionChangedMessage(
                            hubConnectorDescription.serialize(),
                            connect
                    )
            );
        } catch (IOException e) {
            Log.d("failed to send hub connection changed message:", e.getLocalizedMessage());
        }
    }

    public void refreshHubList() {
        this.asapActivity.sendMessage2Service(MessageFactory.createAskForActiveHubConnections());
    }
}
