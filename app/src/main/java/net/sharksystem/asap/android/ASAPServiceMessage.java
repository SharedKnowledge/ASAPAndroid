package net.sharksystem.asap.android;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.util.Helper;

import java.util.Collection;

import static net.sharksystem.asap.android.ASAPServiceMethods.ERA_TAG_NOT_SET;
import static net.sharksystem.asap.android.ASAPServiceMethods.READABLE_NAME_TAG;

public class ASAPServiceMessage {
    private static final CharSequence DEFAULT_CLOSED_MAKAN_NAME = "closed makan";
    private int messageNumber;
    private String format = null;
    private String uri = null;
    private Collection<CharSequence> recipients = null;
    private int era = ERA_TAG_NOT_SET;
    private byte[] message = null;
    private String readableName;

    private ASAPServiceMessage(int messageNumber) {
        this.messageNumber = messageNumber;
    }

    /**
     * Create a message without parameters
     * @param methodeID id @see ASAPServiceMethods
     * @return
     */
    public static ASAPServiceMessage createMessage(int methodeID) {
        return new ASAPServiceMessage(methodeID);
    }

    public static ASAPServiceMessage createSendMessage(
            CharSequence appFormat, CharSequence uri, byte[] message) throws ASAPException {
        return createSendMessage(appFormat, uri, message, ERA_TAG_NOT_SET);
    }

    public static ASAPServiceMessage createSendMessage(
            CharSequence appFormat, CharSequence uri, byte[] message, int era) throws ASAPException {
        ASAPServiceMessage asapServiceMessage =
                new ASAPServiceMessage(ASAPServiceMethods.SEND_MESSAGE);
        asapServiceMessage.setupSendMessage(appFormat, uri, message, era);
        return asapServiceMessage;
    }

    public static ASAPServiceMessage createCreateClosedChannelMessage(
            CharSequence appFormat, CharSequence uri, Collection<CharSequence> recipients)
            throws ASAPException {
        return ASAPServiceMessage.createCreateClosedChannelMessage(
                appFormat, uri, DEFAULT_CLOSED_MAKAN_NAME, recipients);
    }

    public static ASAPServiceMessage createCreateClosedChannelMessage(
                CharSequence appFormat, CharSequence uri, CharSequence readableName,
                Collection<CharSequence> recipients)
            throws ASAPException {

        ASAPServiceMessage asapServiceMessage =
                new ASAPServiceMessage(ASAPServiceMethods.CREATE_CLOSED_CHANNEL);
        asapServiceMessage.setupCreateClosedChannelMessage(appFormat, uri, readableName, recipients);
        return asapServiceMessage;
    }

    public Message getMessage() {
        Message msg = Message.obtain(null, this.messageNumber, 0, 0);

        Bundle bundle = new Bundle();
        boolean setAnything = false;

        if(this.format != null && this.format.length() > 0) {
            bundle.putString(ASAPServiceMethods.FORMAT_TAG, this.format); setAnything = true;
        }
        if(this.uri != null && this.uri.length() > 0) {
            bundle.putString(ASAPServiceMethods.URI_TAG, this.uri); setAnything = true;
        }
        if(this.message != null && this.message.length > 0) {
            bundle.putByteArray(ASAPServiceMethods.ASAP_MESSAGE_TAG,this.message); setAnything=true;
        }
        if(this.era != ERA_TAG_NOT_SET) {
            bundle.putInt(ASAPServiceMethods.ERA_TAG, this.era); setAnything = true;
        }
        if(this.recipients != null && this.readableName.length() > 0) {
            bundle.putString(ASAPServiceMethods.RECIPIENTS_TAG,
                    Helper.collection2String(this.recipients)); setAnything = true;
        }
        if(this.readableName != null && this.readableName.length() > 0) {
            bundle.putString(ASAPServiceMethods.READABLE_NAME_TAG, this.readableName);
                    setAnything = true;
        }

        if(setAnything) { msg.setData(bundle); }

        return  msg;
    }

    /////////////////////////////////////////////////////////////////////////////////
    //                                     set ups                                 //
    /////////////////////////////////////////////////////////////////////////////////

    private void setupSendMessage(CharSequence format, CharSequence uri, byte[] message, int era)
            throws ASAPException {
        this.setupFormatAndUri(format, uri);
        this.setupEra(era);

        if(message == null || message.length < 1) {
            throw new ASAPException("message must not be null/empty");
        }

        this.message = message;
    }

    private void setupCreateClosedChannelMessage(CharSequence format, CharSequence uri,
                                 CharSequence readableName,
                                 Collection<CharSequence> recipients) throws ASAPException {

        this.setupFormatAndUri(format, uri);

        // check on empty recipient list
        if(recipients == null || recipients.isEmpty()) {
            throw new ASAPException("recipients must not be null/empty");
        }

        this.readableName = readableName.toString();
        this.recipients = recipients;
    }

    private void setupFormatAndUri(CharSequence format, CharSequence uri) throws ASAPException {
        // TODO: some format tests?
        if(format == null || format.length() < 1) {
            throw new ASAPException("format must not be null/empty");
        }

        if(uri == null || uri.length() < 1) {
            throw new ASAPException("format must not be null/empty");
        }

        this.format = format.toString();
        this.uri = uri.toString();
    }

    private void setupEra(int era) {
        // TODO: some range tests?
        this.era = era;
    }

    /////////////////////////////////////////////////////////////////////////////////
    //                                     parsing                                 //
    /////////////////////////////////////////////////////////////////////////////////

    /////////////////////// real methods
    public static ASAPServiceMessage createASAPServiceMessage(Message msg) throws ASAPException {
        ASAPServiceMessage asapServiceMessage = new ASAPServiceMessage(msg.what);

        switch(msg.what) {
            case ASAPServiceMethods.SEND_MESSAGE:
                asapServiceMessage.parseSendMessage(msg);
                break;

            case ASAPServiceMethods.CREATE_CLOSED_CHANNEL:
                asapServiceMessage.parseCreateClosedChannel(msg);
                break;

            default: // nothing
        }

        return asapServiceMessage;
    }

    /** mandatory: format, uri, message
     * optional: era
     */
    private void parseSendMessage(Message msg) throws ASAPException {
        Bundle msgData = this.parseBundle(msg);
        this.parseFormatAndUri(msgData, true);
        this.parseASAPMessage(msgData, true);
        this.parseEra(msgData, false);
    }

    /** mandatory: format, uri, recipients */
    private void parseCreateClosedChannel(Message msg) throws ASAPException {
        Bundle msgData = this.parseBundle(msg);
        this.parseFormatAndUri(msgData, true);
        parseRecipients(msgData, true);
        parseReadableName(msgData, true);
    }

    /////////////////////// helper
    private void parseFormatAndUri(Bundle msgData, boolean mandatory) throws ASAPException {
        this.format = msgData.getString(ASAPServiceMethods.FORMAT_TAG);
        this.uri = msgData.getString(ASAPServiceMethods.URI_TAG);

        if(mandatory
                && (
                (this.format == null || this.format.length() < 1)
                        ||  (this.uri == null || this.uri.length() < 1)
        )
        ) {
            throw new ASAPException("format and uri must not be null");
        }
    }

    private void parseASAPMessage(Bundle msgData, boolean mandatory) throws ASAPException {
        this.message = msgData.getByteArray(ASAPServiceMethods.ASAP_MESSAGE_TAG);

        if(mandatory && ( (this.message == null || this.message.length < 1) ) ) {
            throw new ASAPException("message must not be null");
        }
    }

    private void parseEra(Bundle msgData, boolean mandatory) throws ASAPException {
        this.era = msgData.getInt(ASAPServiceMethods.ERA_TAG, ERA_TAG_NOT_SET);

        if(mandatory && this.era == ERA_TAG_NOT_SET) {
            throw new ASAPException("era must be set");
        }
    }

    private void parseReadableName(Bundle msgData, boolean mandatory) throws ASAPException {
        this.readableName = msgData.getString(READABLE_NAME_TAG);

        if(mandatory && this.readableName == null) {
            throw new ASAPException("readable must be set");
        }
    }

    private void parseRecipients(Bundle msgData, boolean mandatory) throws ASAPException {
        String recipientsString = msgData.getString(ASAPServiceMethods.RECIPIENTS_TAG);
        if(recipientsString != null && recipientsString.length() > 0) {
            this.recipients = Helper.string2CharSequenceSet(recipientsString);
        } else {
            if(mandatory) {
                throw new ASAPException("recipients list must not be null/empty");
            }
        }
    }

    private Bundle parseBundle(Message msg) throws ASAPException {
        Bundle msgData = msg.getData();
        if (msgData == null) {
            Log.e(this.getLogStart(), "send message must contain parameters");
            throw new ASAPException("send message must contain parameters");
        }
        return msgData;
    }

    /////////////////////////////////////////////////////////////////////////////////
    //                                     others                                 //
    /////////////////////////////////////////////////////////////////////////////////
    private String getLogStart() {
        return net.sharksystem.asap.util.Log.startLog(this).toString();
    }

    public CharSequence getFormat() { return this.format; }

    public Collection<CharSequence> getRecipients() { return this.recipients;}
    public boolean isRecipientsListSet() {
        return (this.recipients != null && !recipients.isEmpty());
    }

    public CharSequence getURI() { return this.uri; }

    public int getEra() { return this.era; }
    public boolean isEraSet() { return this.era != ERA_TAG_NOT_SET; }

    public byte[] getASAPMessage() { return this.message; }
    public boolean isASAPMessageSet() { return this.message != null && this.message.length > 0;}
}
