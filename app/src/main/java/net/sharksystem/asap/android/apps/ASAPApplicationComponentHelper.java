package net.sharksystem.asap.android.apps;

import android.content.Context;

import net.sharksystem.asap.ASAPException;

public class ASAPApplicationComponentHelper {
    private ASAPApplication asapApplication;

    /**
     * set asap application which can be used from component e.g. to add listeners etc.
     * @param asapApplication
     */
    public void setASAPApplication(ASAPApplication asapApplication) {
        this.asapApplication = asapApplication;
    }

    public ASAPApplication getASAPApplication() {
        return this.asapApplication;
    }

    public Context getContext() throws ASAPException {
        if(this.asapApplication == null) {
            throw new ASAPException("ASAPApplication not yet set, fatal");
        }

        return this.asapApplication.getActivity();
    }

    private String getLogStart() {
        return net.sharksystem.asap.util.Log.startLog(this).toString();
    }
}
