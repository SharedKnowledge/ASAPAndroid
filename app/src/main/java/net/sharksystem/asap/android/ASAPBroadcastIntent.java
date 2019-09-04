package net.sharksystem.asap.android;

import android.content.Context;
import android.content.Intent;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.service.ASAPService;

public class ASAPBroadcastIntent extends Intent {

    private CharSequence folder;
    private CharSequence uri;
    private int era;
    private CharSequence user;

    public ASAPBroadcastIntent(CharSequence user, CharSequence folderName,
                               CharSequence uri, int eraInt) throws ASAPException {
        super();

        if(folderName == null || uri == null || user == null)
            throw new ASAPException("parameters must no be null");

        this.setAction(ASAP.BROADCAST_ACTION);

        this.putExtra(ASAP.ERA, eraInt);
        this.putExtra(ASAP.FOLDER, folderName);
        this.putExtra(ASAP.URI, uri);
        this.putExtra(ASAP.USER, user);

        this.folder = folderName;
        this.uri = uri;
        this.era = era;
        this.user = user;
    }

    public ASAPBroadcastIntent(Intent intent) throws ASAPException {
        super();

        // just parse extras
        this.folder = intent.getStringExtra(ASAP.FOLDER);
        this.uri = intent.getStringExtra(ASAP.URI);
        this.era = intent.getIntExtra(ASAP.ERA, 0);
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
}
