package net.sharksystem.aasp.android.example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import net.sharksystem.aasp.android.AASP;
import net.sharksystem.asp3.ASP3ChunkStorage;
import net.sharksystem.asp3.ASP3EngineFS;
import net.sharksystem.asp3.ASP3Exception;

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
            ASP3ChunkStorage chunkStorage = ASP3EngineFS.getASP3ChunkStorage(folder);
            Toast.makeText(context, "got storage on client side", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ASP3Exception e) {
            e.printStackTrace();
        }
    }
}
