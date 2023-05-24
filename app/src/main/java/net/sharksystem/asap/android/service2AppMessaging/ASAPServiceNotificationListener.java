package net.sharksystem.asap.android.service2AppMessaging;

import net.sharksystem.hub.peerside.HubConnectorDescription;

import java.util.List;
import java.util.Set;

public interface ASAPServiceNotificationListener {
    void asapNotifyBTDiscoverableStopped();

    void asapNotifyBTDiscoveryStopped();

    void asapNotifyBTDiscoveryStarted();

    void asapNotifyBTDiscoverableStarted();

    void asapNotifyBTEnvironmentStarted();

    void asapNotifyBTEnvironmentStopped();

    void asapNotifyOnlinePeersChanged(Set<CharSequence> peerList);

    void asapNotifyHubsConnected();

    void asapNotifyHubsDisconnected();

    void asapNotifyHubListAvailable(List<HubConnectorDescription> hubConnectorDescriptions);
}
