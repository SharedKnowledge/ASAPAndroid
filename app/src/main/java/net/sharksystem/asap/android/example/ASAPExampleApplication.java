package net.sharksystem.asap.android.example;

import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.android.apps.ASAPApplication;

import java.util.Collection;

public class ASAPExampleApplication extends ASAPApplication {
    private CharSequence id;

    public ASAPExampleApplication(Collection<CharSequence> formats) {
        super(formats);
        this.id = ASAP.createUniqueID();
    }

    public CharSequence getASAPOwnerID() {
        return this.id;
    }
}
