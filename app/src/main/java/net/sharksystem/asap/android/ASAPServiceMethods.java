package net.sharksystem.asap.android;

public class ASAPServiceMethods {

    ///////////// sending asap messages

    /** mandatory: format, uri, message
     * optional: era
     */
    public static final int SEND_MESSAGE = 0;
    /** mandatory: format, uri, recipients */
    public static final int CREATE_CLOSED_CHANNEL = 1;

    ///////////// layer 2 protocol management
    public static final int START_WIFI_DIRECT = 100;
    public static final int STOP_WIFI_DIRECT = 101;

    public static final int START_BLUETOOTH = 102;
    public static final int STOP_BLUETOOTH = 103;
    public static final int START_BLUETOOTH_DISCOVERABLE = 104;
    public static final int START_BLUETOOTH_DISCOVERY = 105;

    // check from time if there is a paired device in the neighbourhood
    public static final int START_RECONNECT_PAIRED_DEVICES = 106;
    public static final int STOP_RECONNECT_PAIRED_DEVICES = 107;

    public static final int START_LORA = 108;
    public static final int STOP_LORA = 109;

    ///////////// asap engine management
    public static final int START_BROADCASTS = 200;
    public static final int STOP_BROADCASTS = 201;
    public static final int ASK_PROTOCOL_STATUS = 202;

    // tags for putting extra data to messages or broadcasts
    public static final String URI_TAG = "ASAP_MESSAGE_URI";
    public static final String ASAP_MESSAGE_TAG = "ASAP_MESSAGE";
    public static final String FORMAT_TAG = "ASAP_FORMAT";
    public static final String ERA_TAG = "ASAP_ERA";
    public static final int ERA_TAG_NOT_SET = -1;
    public static final String RECIPIENTS_TAG = "ASAP_RECIPIENTS";
    public static final String READABLE_NAME_TAG = "ASAP_READABLE_NAME";
    public static final String PERSISTENT = "ASAP_PERSISTENT";
}
