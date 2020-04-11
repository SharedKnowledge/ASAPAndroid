package net.sharksystem.asap.android.example;

import net.sharksystem.asap.android.apps.ASAPActivity;
import net.sharksystem.asap.android.apps.ASAPApplication;

import java.util.ArrayList;
import java.util.Collection;

public class ASAPExampleRootActivity extends ASAPActivity {
    public static final String ASAP_EXAMPLE_APPNAME = "ASAP_EXAMPLE_APP";
    private static ASAPApplication asapApplication;

    static {
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(ASAP_EXAMPLE_APPNAME);
        ASAPExampleRootActivity.asapApplication = new ASAPExampleApplication(formats);
    }

    public ASAPExampleRootActivity() {
        super(ASAPExampleRootActivity.asapApplication);
    }
}
