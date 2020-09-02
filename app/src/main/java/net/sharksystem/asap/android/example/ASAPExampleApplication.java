package net.sharksystem.asap.android.example;

import android.app.Activity;

import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.android.apps.ASAPApplication;

import java.util.ArrayList;
import java.util.Collection;

public class ASAPExampleApplication extends ASAPApplication {
    public static final String ASAP_EXAMPLE_APPNAME = "ASAP_EXAMPLE_APP";
    private CharSequence id;
    static ASAPExampleApplication instance = null;

    static ASAPExampleApplication initializeASAPExampleApplication(Activity initialActivity) {
        if(ASAPExampleApplication.instance == null) {
            Collection<CharSequence> formats = new ArrayList<>();
            formats.add(ASAP_EXAMPLE_APPNAME);

            // create
            ASAPExampleApplication.instance = new ASAPExampleApplication(formats, initialActivity);

            // there could be some other steps. Setting up sub components. But there are non here.

            // launch
            ASAPExampleApplication.instance.startASAPApplication();
        } // else - already initialized - nothing happens.

        return ASAPExampleApplication.instance;
    }

    private ASAPExampleApplication(Collection<CharSequence> formats, Activity initialActivity) {
        super(formats, initialActivity);

        this.id = ASAP.createUniqueID();
    }

    public CharSequence getOwnerID() {
        return this.id;
    }
}
