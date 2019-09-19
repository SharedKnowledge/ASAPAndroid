package net.sharksystem.asap.android.service2AppMessaging;

import android.content.Intent;

public class ASAPServiceRequestNotifyIntent extends Intent {
    public static final String ASAP_SERVICE_REQUEST_ACTION = "net.sharksystem.asap.util.servicerequest";
    public static final String ASAP_REQUEST_COMMAND = "ASAP_REQUEST_COMMAND";

    public static final int ASAP_RQ_ASK_USER_TO_ENABLE_BLUETOOTH = 0;
    public static final int ASAP_RQ_ASK_USER_TO_START_BT_DISCOVERABLE = 1;

    public static final int ASAP_NOTIFY_BT_DISCOVERY_STOPPED = 100;
    public static final int ASAP_NOTIFY_BT_DISCOVERABLE_STOPPED = 101;
    public static final int ASAP_NOTIFY_BT_DISCOVERY_STARTED = 102;
    public static final int ASAP_NOTIFY_BT_DISCOVERABLE_STARTED = 103;
    public static final int ASAP_NOTIFY_BT_ENVIRONMENT_STARTED = 104;
    public static final int ASAP_NOTIFY_BT_ENVIRONMENT_STOPPED = 105;
    public static final int ASAP_NOTIFY_ONLINE_PEERS_CHANGED = 106;

    public static final String ASAP_PARAMETER_1 = "ASAP_PARAMETER_1";

    public ASAPServiceRequestNotifyIntent(int request) {
        super();

        this.setAction(ASAP_SERVICE_REQUEST_ACTION);

        this.putExtra(ASAP_REQUEST_COMMAND, request);
    }

    public ASAPServiceRequestNotifyIntent(int requestType, int intParameter) {
        this(requestType);

        this.putExtra(ASAP_PARAMETER_1, intParameter);
    }
}
