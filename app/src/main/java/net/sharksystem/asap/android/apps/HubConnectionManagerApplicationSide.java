package net.sharksystem.asap.android.apps;

import android.util.Log;

import net.sharksystem.SharkException;
import net.sharksystem.asap.android.app2serviceMessaging.MessageFactory;
import net.sharksystem.hub.BasicHubConnectionManager;
import net.sharksystem.hub.HubConnectionManager;
import net.sharksystem.hub.peerside.HubConnectorDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HubConnectionManagerApplicationSide
        extends BasicHubConnectionManager
        implements HubConnectionManager {

    private final ASAPActivity asapActivity;
    private final List<HubManagerStatusChangedListener> listener = new ArrayList<>();


    public HubConnectionManagerApplicationSide(ASAPActivity asapActivity) {
        this.asapActivity = asapActivity;
    }

    /**
     *  Establishes a connection an ASAPHub
     * @param hcd HubConnectorDescription containing the connection information
     * @throws SharkException
     * @throws IOException
     */
    public void connectHub(HubConnectorDescription hcd) throws SharkException, IOException {
        this.syncLists();
        super.connectHub(hcd);
        this.connectionChanged(hcd, true);
    }

    public void disconnectHub(HubConnectorDescription hcd) throws SharkException, IOException {
        this.syncLists();
        super.disconnectHub(hcd);
        this.connectionChanged(hcd, false);
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

    /**
     * Is called by the ASAPActivity after receiving a broadcast message as response to the
     * request sent in refreshHubList()
     * @param hubConnectorDescriptions
     */
    public void updateHubList(List<HubConnectorDescription> hubConnectorDescriptions){
        this.hcdListHub = hubConnectorDescriptions;
        for(HubManagerStatusChangedListener l : listener) {
            l.notifyHubListReceived();
        }
    }

    public void addListener(HubManagerStatusChangedListener listener){
        this.listener.add(listener);
    }

    public void removeListener(HubManagerStatusChangedListener listener) {
        this.listener.remove(listener);
    }

}
