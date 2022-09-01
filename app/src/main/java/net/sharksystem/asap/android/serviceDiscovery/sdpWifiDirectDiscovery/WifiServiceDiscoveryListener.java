package net.sharksystem.asap.android.serviceDiscovery.sdpWifiDirectDiscovery;

import android.net.wifi.p2p.WifiP2pDevice;

import net.sharksystem.asap.android.serviceDiscovery.serviceDescription.ServiceDescription;


/**
 * Listener interface to get notified when
 * the {@link SdpWifiDirectDiscoveryEngine} found a
 * new device or service on a device.
 *
 * @author WilliBolke
 */
public interface WifiServiceDiscoveryListener
{
    /**
     * Called whenever a service was discovered which has been looked for
     * through {@link SdpWifiDirectDiscoveryEngine#startSdpDiscoveryForService(ServiceDescription)}
     * <p>
     * Provides the remote host device and the service description to identify the service
     *
     * @param host
     *         The BluetoothDevice hosting / advertising the service
     * @param description
     *         The service description as provided in {@link SdpWifiDirectDiscoveryEngine#startSdpDiscoveryForService(ServiceDescription)}
     *         which was resolved through matching UUIDs
     */
    void onServiceDiscovered(WifiP2pDevice host, ServiceDescription description);
}
