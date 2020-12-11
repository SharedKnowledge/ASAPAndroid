package net.sharksystem.asap.android.service2AppMessaging;

import java.util.Set;

public interface ASAPServiceNotificationListener {
    void asapNotifyBTDiscoverableStopped();

    void asapNotifyBTDiscoveryStopped();

    void asapNotifyBTDiscoveryStarted();

    void asapNotifyBTDiscoverableStarted();

    void asapNotifyBTEnvironmentStarted();

    void asapNotifyBTEnvironmentStopped();

    void asapNotifyOnlinePeersChanged(Set<CharSequence> peerList);
}
