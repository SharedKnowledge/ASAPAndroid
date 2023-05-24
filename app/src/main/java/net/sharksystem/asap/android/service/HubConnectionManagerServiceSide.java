package net.sharksystem.asap.android.service;

import static net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent.ASAP_PARAMETER_1;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPEncounterManager;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;
import net.sharksystem.hub.BasicHubConnectionManager;
import net.sharksystem.hub.HubConnectionManagerMessageHandler;
import net.sharksystem.hub.peerside.ASAPHubManagerImpl;
import net.sharksystem.hub.peerside.HubConnectorDescription;
import net.sharksystem.hub.peerside.TCPHubConnectorDescriptionImpl;

import java.io.IOException;

public class HubConnectionManagerServiceSide
        extends BasicHubConnectionManager
        implements HubConnectionManagerMessageHandler {
    private final ASAPPeer asapPeer;
    private ASAPHubManagerImpl hubManager;
    private Context context;

    public HubConnectionManagerServiceSide(ASAPEncounterManager encounterManager, ASAPPeer asapPeer,
                                           Context context) {
        this.asapPeer = asapPeer;
        this.hubManager = new ASAPHubManagerImpl(encounterManager);
        this.context = context;
    }

    @Override
    public void connectionChanged(HubConnectorDescription hcd, boolean connect) throws SharkException, IOException {
        // sync lists
        if(hcd == null) {
            // TODO
            return;
        }
        if(connect) super.connectHub(hcd);
        this.hubManager.connectASAPHubs(this.hcdList, this.asapPeer, true);
    }

    @Override
    public void refreshHubList() {
        this.hcdListHub = this.hubManager.getRunningConnectorDescriptions();
        // broadcast to application
        Intent intent = new ASAPServiceRequestNotifyIntent(
                ASAPServiceRequestNotifyIntent.ASAP_NOTIFY_HUB_LIST_AVAILABLE);
        try {
            intent.putExtra(ASAP_PARAMETER_1, TCPHubConnectorDescriptionImpl.serializeConnectorDescriptionList(this.hcdListHub));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.context.sendBroadcast(intent);
    }
}
