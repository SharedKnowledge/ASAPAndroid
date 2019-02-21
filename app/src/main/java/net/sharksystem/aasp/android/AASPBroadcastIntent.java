package net.sharksystem.aasp.android;

import android.content.Intent;

import net.sharksystem.aasp.AASPException;

public class AASPBroadcastIntent extends Intent {

    private CharSequence folder;
    private CharSequence uri;
    private int era;
    private CharSequence user;

    public AASPBroadcastIntent(CharSequence user, CharSequence folderName, CharSequence uri,
                               int eraInt) throws AASPException {
        super();

        if(folderName == null || uri == null || user == null)
            throw new AASPException("parameters must no be null");

        this.setAction(AASP.BROADCAST_ACTION);

        this.putExtra(AASP.ERA, eraInt);
        this.putExtra(AASP.FOLDER, folderName);
        this.putExtra(AASP.URI, uri);
        this.putExtra(AASP.USER, user);

        this.folder = folderName;
        this.uri = uri;
        this.era = era;
        this.user = user;
    }

    public AASPBroadcastIntent(Intent intent) throws AASPException {
        super();

        // just parse extras
        this.folder = intent.getStringExtra(AASP.FOLDER);
        this.uri = intent.getStringExtra(AASP.URI);
        this.era = intent.getIntExtra(AASP.ERA, 0);
        this.user = intent.getStringExtra(AASP.USER);
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
