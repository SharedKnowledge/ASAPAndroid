package net.sharksystem.asap.android;

import android.os.Bundle;
import android.os.Message;

public class ASAPServiceMethods {
    public static final int SEND_MESSAGE = 0;
    public static final int ASK_PROTOCOL_STATUS = 9;

    public static final int START_WIFI_DIRECT = 1;
    public static final int STOP_WIFI_DIRECT = 2;

    public static final int START_BLUETOOTH = 3;
    public static final int STOP_BLUETOOTH = 4;
    public static final int START_BLUETOOTH_DISCOVERABLE = 5;
    public static final int START_BLUETOOTH_DISCOVERY = 6;

    public static final int START_BROADCASTS = 7;
    public static final int STOP_BROADCASTS = 8;
}
