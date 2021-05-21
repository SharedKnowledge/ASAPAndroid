package net.sharksystem.asap.android;

import android.content.Intent;

import net.sharksystem.SharkNotSupportedException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.utils.Log;

import java.io.IOException;

public class ASAPChunkReceivedBroadcastIntent extends Intent {

    private final ASAPHop asapHop;
    private CharSequence folder;
    private CharSequence uri;
    private int era;
    private CharSequence senderE2E;
    private CharSequence format;

    public ASAPChunkReceivedBroadcastIntent(CharSequence format, CharSequence senderE2E,
                CharSequence folderName,
                CharSequence uri, int era,
                ASAPHop asapHop) throws ASAPException {

        super();

        if(format == null || folderName == null || uri == null || senderE2E == null)
            throw new ASAPException("parameters must no be null");

        this.setAction(ASAPAndroid.ASAP_CHUNK_RECEIVED_ACTION);

        this.putExtra(ASAPServiceMethods.ERA_TAG, era);
        this.putExtra(ASAPServiceMethods.FORMAT_TAG, format);
        this.putExtra(ASAPAndroid.FOLDER, folderName);
        this.putExtra(ASAPServiceMethods.URI_TAG, uri);
        this.putExtra(ASAPAndroid.SENDER_E2E, senderE2E);
        try {
            byte[] asapHopBytes = ASAPSerialization.asapHop2ByteArray(asapHop);
            this.putExtra(ASAPAndroid.ASAP_HOP, asapHopBytes);
        }
        catch(IOException e) {
            // ignore
            Log.writeLogErr(this, "cannot serialize ASAPHop: " + asapHop);
        }

        this.format = format;
        this.folder = folderName;
        this.uri = uri;
        this.era = era;
        this.senderE2E = senderE2E;
        this.asapHop = asapHop;
    }

    public ASAPChunkReceivedBroadcastIntent(Intent intent) throws ASAPException, IOException {
        super();

        // just parse extras
        this.format = intent.getStringExtra(ASAPServiceMethods.FORMAT_TAG);
        this.folder = intent.getStringExtra(ASAPAndroid.FOLDER);
        this.uri = intent.getStringExtra(ASAPServiceMethods.URI_TAG);
        this.era = intent.getIntExtra(ASAPServiceMethods.ERA_TAG, 0);
        this.senderE2E = intent.getStringExtra(ASAPAndroid.SENDER_E2E);
        byte[] asapHopBytes = intent.getByteArrayExtra(ASAPAndroid.ASAP_HOP);
        this.asapHop = ASAPSerialization.byteArray2ASAPHop(asapHopBytes);
    }

    public CharSequence getFoldername() {
        return this.folder;
    }

    public CharSequence getUri() {
        return this.uri;
    }

    public CharSequence getSenderE2E() {
        // TODO E2ESender == owner???
        return this.senderE2E;
    }

    public int getEra() {
        return this.era;
    }

    public CharSequence getFormat() { return this.format; }

    public ASAPHop getASAPHop() {
        return this.asapHop;
    }
}
