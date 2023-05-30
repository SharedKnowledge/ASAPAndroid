package net.sharksystem.asap.android.apps;

/**
 * interface for implementing a listener which notifies about an async response from the
 * HubConnectionManagerServiceSide.java
 */
public interface HubManagerStatusChangedListener {

    /**
     * This method should be called after receiving a Broadcast message from the
     * HubConnectionManagerServiceSide about the connected hubs
     */
    public void notifyHubListReceived();

}
