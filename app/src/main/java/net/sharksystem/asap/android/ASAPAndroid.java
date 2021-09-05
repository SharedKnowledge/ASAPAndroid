package net.sharksystem.asap.android;

import android.Manifest;

import java.util.ArrayList;
import java.util.List;

/**
 * Be sure to have the ASAPEngine in one of your library directories!
 */
public class ASAPAndroid {
    public static final String SENDER_E2E = "senderE2E";
    public static final String FOLDER = "folder";
    public static final String RECEIVER = "receiver";
    public static final String ASAP_HOPS = "asapHops";

    public static final String ASAP_CHUNK_RECEIVED_ACTION = "net.sharksystem.asap.received";

    public static final int PORT_NUMBER = 7777;

    public static final String ONLINE_EXCHANGE_PARAMETER_NAME = "ASAP_ONLINE_EXCHANGE";
    public static final String MAX_EXECUTION_TIME_PARAMETER_NAME = "ASAP_MAX_EXECUTION_TIME";
    public static final String SUPPORTED_FORMATS_PARAMETER_NAME = "ASAP_SUPPORTED_FORMATS";

    public static final List<String> requiredPermissions = new ArrayList<>();

    static {
        ASAPAndroid.requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ASAPAndroid.requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ASAPAndroid.requiredPermissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        ASAPAndroid.requiredPermissions.add(Manifest.permission.CHANGE_WIFI_STATE);
        ASAPAndroid.requiredPermissions.add(Manifest.permission.CHANGE_NETWORK_STATE);
        ASAPAndroid.requiredPermissions.add(Manifest.permission.INTERNET);
        ASAPAndroid.requiredPermissions.add(Manifest.permission.BLUETOOTH);
        ASAPAndroid.requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        ASAPAndroid.requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        ASAPAndroid.requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
    }
}
