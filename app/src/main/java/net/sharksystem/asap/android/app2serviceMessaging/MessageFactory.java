package net.sharksystem.asap.android.app2serviceMessaging;

import android.os.Bundle;
import android.os.Message;

import net.sharksystem.asap.android.ASAPServiceMessage;
import net.sharksystem.asap.android.ASAPServiceMethods;

public class MessageFactory {

    /////////////////// ASAP hub management
    public static Message createConnectHubMessage(byte[] hubDescription) {
        Message msg = Message.obtain(null, ASAPServiceMethods.CONNECT_ASAP_HUBS, 0, 0);
        return addHubDescription(msg, hubDescription);
    }

    public static Message createDisconnectHubMessage(byte[] hubDescription) {
        Message msg = Message.obtain(null, ASAPServiceMethods.DISCONNECT_ASAP_HUBS, 0, 0);
        return addHubDescription(msg, hubDescription);
    }

    private static Message addHubDescription(Message msg, byte[] hubDescription) {
        Bundle bundle = new Bundle();
        bundle.putByteArray(ASAPServiceMessage.HUB_CONNECTOR_DESCRIPTION_TAG, hubDescription);
        msg.setData(bundle);
        return  msg;
    }
}
