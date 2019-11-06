package net.sharksystem.asap.android.apps;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import net.sharksystem.asap.ASAPAbstractOnlineMessageSender;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.ASAP;
import net.sharksystem.asap.android.ASAPServiceMethods;
import net.sharksystem.asap.android.Util;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ASAPOnlineMessageSenderAndroidUserSide extends ASAPAbstractOnlineMessageSender {

    private final ASAPApplication asapApplication;

    public ASAPOnlineMessageSenderAndroidUserSide(ASAPApplication asapApplication) {
        this.asapApplication = asapApplication;
    }

    @Override
    public void sendASAPAssimilate(CharSequence format, CharSequence uri,
                                   Set<CharSequence> recipients, byte[] messageAsBytes,
                                   int era) throws ASAPException {

        Message msg;

        Log.d(this.getLogStart(), "sendASAPAssimilate called");
        if(recipients == null || recipients.size() < 1) {
            Log.w(this.getLogStart(), "send to anonymous recipients");
            if(this.asapApplication.getOnlinePeerList().size() < 1) {
                Log.d(this.getLogStart(), "no online peers at all - send nothing");
            } else {
                msg = this.createSendASAPMessageMessage(
                        format, uri, null, messageAsBytes, era);

                this.asapApplication.getActivity().sendMessage2Service(msg);
            }
        } else {
            for (CharSequence recipient : recipients) {
                if(this.isOnline(recipient)) {
                    msg = this.createSendASAPMessageMessage(format, uri, recipient, messageAsBytes, era);
                    this.asapApplication.getActivity().sendMessage2Service(msg);
                } else {
                    Log.d(this.getLogStart(), "no message sent / peer not online: " + recipient);
                }
            }
        }
    }

    private boolean isOnline(CharSequence peer) {
        // TODO: need string comparision a la equalsIgnoreCase?
        Log.d(this.getLogStart(), "TODO: need string comparision a la equalsIgnoreCase?");
        return this.asapApplication.getOnlinePeerList().contains(peer);
    }


    @Override
    public void sendASAPAssimilate(CharSequence format, CharSequence uri, byte[] messageAsBytes, int era)
            throws IOException, ASAPException {

        this.sendASAPAssimilate(format, uri, (CharSequence) null, messageAsBytes, era);
    }

    private Message createSendASAPMessageMessage(CharSequence format, CharSequence uri,
                                                 CharSequence recipient, byte[] asapMessage,
                                                 int era) throws ASAPException {

        if(format == null || format.length() < 1
                || uri == null || uri.length() < 1
                || asapMessage == null || asapMessage.length < 1
        ) {
            throw new ASAPException("format, uri, message cannot be null or empty");
        }

        Message msg = Message.obtain(null, ASAPServiceMethods.SEND_MESSAGE, 0, 0);
        Bundle msgData = new Bundle();
        msgData.putCharSequence(ASAP.FORMAT, format);
        msgData.putCharSequence(ASAP.URI, uri);
        if(recipient != null) { msgData.putCharSequence(ASAP.RECIPIENT, recipient); }
        msgData.putByteArray(ASAP.MESSAGE_CONTENT, asapMessage);
        msgData.putInt(ASAP.ERA, era);
        msg.setData(msgData);

        return msg;
    }

    private String getLogStart() {
        return Util.getLogStart(this);
    }
}
