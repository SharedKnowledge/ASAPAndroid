package net.sharksystem.asap.android.service2AppMessaging;

public interface ASAPServiceNotificationListener {
    void asapNotifyBTDiscoverableStopped();

    void aspNotifyBTDiscoveryStopped();

    void aspNotifyBTDiscoveryStarted();

    void aspNotifyBTDiscoverableStarted();

    void aspNotifyBTEnvironmentStarted();

    void aspNotifyBTEnvironmentStopped();
}
