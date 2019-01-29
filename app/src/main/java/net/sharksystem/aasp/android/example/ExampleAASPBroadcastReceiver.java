package net.sharksystem.aasp.android.example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import net.sharksystem.aasp.AASPChunkStorage;
import net.sharksystem.aasp.AASPEngineFS;
import net.sharksystem.aasp.AASPException;
import net.sharksystem.aasp.AASPStorage;
import net.sharksystem.aasp.android.AASP;

import java.io.IOException;

public class ExampleAASPBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String folder = intent.getStringExtra(AASP.FOLDER);
        String uri = intent.getStringExtra(AASP.URI);
        int era = intent.getIntExtra(AASP.ERA, 0);

        String text = "AASPService notified: "
                + folder + " / "
                + uri + " / "
                + era;

        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

        // create access to that chunk storage
        try {
            AASPStorage chunkStorage = AASPEngineFS.getAASPChunkStorage(folder);
            Toast.makeText(context, "got storage on client side", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AASPException e) {
            e.printStackTrace();
        }
    }
}
