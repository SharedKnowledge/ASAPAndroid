package net.sharksystem.asap.android.example;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.apps.ASAPAndroidPeer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ASAPInitialExampleActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // initialize ASAP peer (application side)
            if(!ASAPAndroidPeer.peerInitialized()) {
                Collection<CharSequence> formats = new ArrayList<>();

                // add a name that identifies your app which also identifies general message format
                formats.add(ExampleAppDefinitions.ASAP_EXAMPLE_APPNAME);

                // generate a name for this peer
                String peerName = ASAP.createUniqueID();

                ASAPAndroidPeer.initializePeer(peerName, formats, this);
            }

            // start ASAP peer (service side)
            if(!ASAPAndroidPeer.peerStarted()) {
                ASAPAndroidPeer.startPeer();
            }
        } catch (IOException | ASAPException e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "fatal: " + e.getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
        }

        // launch real first activity
        this.finish();
        Intent intent = new Intent(this, ASAPExampleActivity.class);
        this.startActivity(intent);
    }
}
