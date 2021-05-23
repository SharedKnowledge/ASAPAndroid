package net.sharksystem.asap.android;

import android.content.Intent;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.util.List;

public class ASAPChunkReceivedBroadcastIntent extends Intent {

    private final List<ASAPHop> asapHops;
    private CharSequence folder;
    private CharSequence uri;
    private int era;
    private CharSequence senderE2E;
    private CharSequence format;

    public ASAPChunkReceivedBroadcastIntent(CharSequence format, CharSequence senderE2E,
                                            CharSequence folderName,
                                            CharSequence uri, int era,
                                            List<ASAPHop> asapHops) throws ASAPException {

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
            byte[] asapHopBytes = ASAPSerialization.asapHopList2ByteArray(asapHops);
            this.putExtra(ASAPAndroid.ASAP_HOPS, asapHopBytes);
        }
        catch(IOException e) {
            // ignore
            Log.writeLogErr(this, "cannot serialize ASAPHop: " + asapHops);
        }

        this.format = format;
        this.folder = folderName;
        this.uri = uri;
        this.era = era;
        this.senderE2E = senderE2E;
        this.asapHops = asapHops;
    }

    public ASAPChunkReceivedBroadcastIntent(Intent intent) throws ASAPException, IOException {
        super();

        // just parse extras
        this.format = intent.getStringExtra(ASAPServiceMethods.FORMAT_TAG);
        this.folder = intent.getStringExtra(ASAPAndroid.FOLDER);
        this.uri = intent.getStringExtra(ASAPServiceMethods.URI_TAG);
        this.era = intent.getIntExtra(ASAPServiceMethods.ERA_TAG, 0);
        this.senderE2E = intent.getStringExtra(ASAPAndroid.SENDER_E2E);
        byte[] asapHopsBytes = intent.getByteArrayExtra(ASAPAndroid.ASAP_HOPS);
        this.asapHops = ASAPSerialization.byteArray2ASAPHopList(asapHopsBytes);
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

    public List<ASAPHop> getASAPHop() {
        return this.asapHops;
    }
}
