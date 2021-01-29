package net.sharksystem.asap.android.apps;

import android.content.Context;

import net.sharksystem.asap.ASAPException;

public class ASAPApplicationComponentHelper {
    private ASAPAndroidPeer asapAndroidPeer;

    /**
     * set asap application which can be used from component e.g. to add listeners etc.
     * @param asapAndroidPeer
     */
    public void setASAPApplication(ASAPAndroidPeer asapAndroidPeer) {
        this.asapAndroidPeer = asapAndroidPeer;
    }

    public ASAPAndroidPeer getASAPApplication() {
        return this.asapAndroidPeer;
    }

    public Context getContext() throws ASAPException {
        if(this.asapAndroidPeer == null) {
            throw new ASAPException("ASAPApplication not yet set, fatal");
        }

        return this.asapAndroidPeer.getActivity();
    }

    private String getLogStart() {
        return net.sharksystem.utils.Log.startLog(this).toString();
    }
}
