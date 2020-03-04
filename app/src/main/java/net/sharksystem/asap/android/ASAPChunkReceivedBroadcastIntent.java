package net.sharksystem.asap.android;

import android.content.Intent;

import net.sharksystem.asap.ASAPException;

public class ASAPChunkReceivedBroadcastIntent extends Intent {

    private CharSequence folder;
    private CharSequence uri;
    private int era;
    private CharSequence user;
    private CharSequence format;

    public ASAPChunkReceivedBroadcastIntent(CharSequence format, CharSequence user, CharSequence folderName,
                                            CharSequence uri, int eraInt) throws ASAPException {
        super();

        if(format == null || folderName == null || uri == null || user == null)
            throw new ASAPException("parameters must no be null");

        this.setAction(ASAP.ASAP_CHUNK_RECEIVED_ACTION);

        this.putExtra(ASAPServiceMethods.ERA_TAG, eraInt);
        this.putExtra(ASAPServiceMethods.FORMAT_TAG, format);
        this.putExtra(ASAP.FOLDER, folderName);
        this.putExtra(ASAPServiceMethods.URI_TAG, uri);
        this.putExtra(ASAP.USER, user);

        this.format = format;
        this.folder = folderName;
        this.uri = uri;
        this.era = era;
        this.user = user;
    }

    public ASAPChunkReceivedBroadcastIntent(Intent intent) throws ASAPException {
        super();

        // just parse extras
        this.format = intent.getStringExtra(ASAPServiceMethods.FORMAT_TAG);
        this.folder = intent.getStringExtra(ASAP.FOLDER);
        this.uri = intent.getStringExtra(ASAPServiceMethods.URI_TAG);
        this.era = intent.getIntExtra(ASAPServiceMethods.ERA_TAG, 0);
        this.user = intent.getStringExtra(ASAP.USER);
    }

    public CharSequence getFoldername() {
        return this.folder;
    }

    public CharSequence getUri() {
        return this.uri;
    }

    public CharSequence getUser() {
        return this.user;
    }

    public int getEra() {
        return this.era;
    }

    public CharSequence getFormat() { return this.format; }
}
