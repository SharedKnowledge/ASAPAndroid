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
import net.sharksystem.aasp.android.AASPBroadcastIntent;

import java.io.IOException;
import java.nio.charset.Charset;

public class ExampleAASPBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            AASPBroadcastIntent aaspIntent = new AASPBroadcastIntent(intent);

            String text = "AASPService notified: "
                    + aaspIntent.getUser() + " / "
                    + aaspIntent.getFoldername() + " / "
                    + aaspIntent.getUri() + " / "
                    + aaspIntent.getEra();

            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

        // create access to that chunk storage
            AASPStorage chunkStorage = AASPEngineFS.getAASPChunkStorage(
                    aaspIntent.getFoldername().toString());

            Toast.makeText(context, "got storage on client side", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AASPException e) {
            e.printStackTrace();
        }
    }
}
