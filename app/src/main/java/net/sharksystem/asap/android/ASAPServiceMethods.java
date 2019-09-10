package net.sharksystem.asap.android;

import android.os.Bundle;
import android.os.Message;

public class ASAPServiceMethods {
    public static final int ADD_MESSAGE = 0;
    public static final int ASK_PROTOCOL_STATUS = 9;

    public static final int START_WIFI_DIRECT = 1;
    public static final int STOP_WIFI_DIRECT = 2;

    public static final int START_BLUETOOTH = 3;
    public static final int STOP_BLUETOOTH = 4;
    public static final int START_BLUETOOTH_DISCOVERABLE = 5;
    public static final int START_BLUETOOTH_DISCOVERY = 6;

    public static final int START_BROADCASTS = 7;
    public static final int STOP_BROADCASTS = 8;

    public static Message createAddASAPMessageMessage(CharSequence format, CharSequence uri,
                                               byte[] asapMessage) {

        Message msg = Message.obtain(null, ASAPServiceMethods.ADD_MESSAGE, 0, 0);
        Bundle msgData = new Bundle();
        msgData.putCharSequence(ASAP.FORMAT, format);
        msgData.putCharSequence(ASAP.URI, uri);
        msgData.putByteArray(ASAP.MESSAGE_CONTENT, asapMessage);
        msg.setData(msgData);

        return msg;
    }
}
