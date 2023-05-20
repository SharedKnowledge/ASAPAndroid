package net.sharksystem.asap.android.app2serviceMessaging;

import android.os.Bundle;
import android.os.Message;

import net.sharksystem.asap.android.ASAPServiceMessage;
import net.sharksystem.asap.android.ASAPServiceMethods;

public class MessageFactory {

    /////////////////// ASAP hub management
    // HUB_CONNECTION_CHANGED (byte[] serializedHubConnectorDescription, boolean connect/disconnect);
    public static Message createHubConnectionChangedMessage(
            byte[] serializedHubConnectorDescription,
            boolean connect) {

        Message msg = Message.obtain(null, ASAPServiceMethods.HUB_CONNECTION_CHANGED, 0, 0);
        Bundle bundle = new Bundle();
        if(serializedHubConnectorDescription != null) {
            bundle.putByteArray(
               ASAPServiceMessage.HUB_CONNECTOR_DESCRIPTION_TAG, serializedHubConnectorDescription);
        }
        bundle.putBoolean(ASAPServiceMethods.BOOLEAN_PARAMETER, connect);
        msg.setData(bundle);
        return msg;
    }

    // ASK_HUB_CONNECTIONS; no parameters.
    public static Message createAskForActiveHubConnections() {
        return Message.obtain(null, ASAPServiceMethods.ASK_HUB_CONNECTIONS, 0, 0);
    }
}
