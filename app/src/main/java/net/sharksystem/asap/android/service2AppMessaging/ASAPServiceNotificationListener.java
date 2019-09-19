package net.sharksystem.asap.android.service2AppMessaging;

import java.util.List;

public interface ASAPServiceNotificationListener {
    void asapNotifyBTDiscoverableStopped();

    void asapNotifyBTDiscoveryStopped();

    void asapNotifyBTDiscoveryStarted();

    void asapNotifyBTDiscoverableStarted();

    void asapNotifyBTEnvironmentStarted();

    void asapNotifyBTEnvironmentStopped();

    void asapNotifyOnlinePeersChanged(List<CharSequence> peerList);
}
