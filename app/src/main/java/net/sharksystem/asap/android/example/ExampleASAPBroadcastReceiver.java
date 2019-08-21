package net.sharksystem.asap.android.example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import net.sharksystem.asap.ASAPEngineFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.asap.android.ASAPBroadcastIntent;

import java.io.IOException;

public class ExampleASAPBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            ASAPBroadcastIntent aaspIntent = new ASAPBroadcastIntent(intent);

            String text = "ASAPService notified: "
                    + aaspIntent.getUser() + " / "
                    + aaspIntent.getFoldername() + " / "
                    + aaspIntent.getUri() + " / "
                    + aaspIntent.getEra();

            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

            // create access to that chunk storage
            ASAPStorage chunkStorage = ASAPEngineFS.getExistingASAPEngineFS(
                    aaspIntent.getFoldername().toString());

            Toast.makeText(context, "got storage on client side", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ASAPException e) {
            e.printStackTrace();
        }
    }
}
