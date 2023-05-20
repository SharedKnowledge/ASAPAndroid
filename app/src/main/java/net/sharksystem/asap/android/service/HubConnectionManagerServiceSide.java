package net.sharksystem.asap.android.service;

import android.util.Log;

import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPEncounterManager;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.hub.BasicHubConnectionManager;
import net.sharksystem.hub.HubConnectionManagerMessageHandler;
import net.sharksystem.hub.peerside.ASAPHubManagerImpl;
import net.sharksystem.hub.peerside.HubConnectorDescription;

import java.io.IOException;

public class HubConnectionManagerServiceSide
        extends BasicHubConnectionManager
        implements HubConnectionManagerMessageHandler {
    private final ASAPPeer asapPeer;
    private ASAPHubManagerImpl hubManager;

    public HubConnectionManagerServiceSide(ASAPEncounterManager encounterManager, ASAPPeer asapPeer) {
        this.asapPeer = asapPeer;
        this.hubManager = new ASAPHubManagerImpl(encounterManager);
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
        // TODO: broadcast to application
    }
}
