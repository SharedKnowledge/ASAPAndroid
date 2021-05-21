package net.sharksystem.asap.android;

import android.content.Intent;

import net.sharksystem.EncounterConnectionType;
import net.sharksystem.SharkNotSupportedException;
import net.sharksystem.asap.ASAPException;

public class ASAPChunkReceivedBroadcastIntent extends Intent {

    private final String senderPoint2Point;
    private final boolean verified;
    private final boolean encrypted;
    private final EncounterConnectionType connectionType;
    private CharSequence folder;
    private CharSequence uri;
    private int era;
    private CharSequence senderE2E;
    private CharSequence format;

    public ASAPChunkReceivedBroadcastIntent(CharSequence format, CharSequence senderE2E,
                CharSequence folderName,
                CharSequence uri, int era,
                String senderPoint2Point, boolean verified, boolean encrypted,
                EncounterConnectionType connectionType) throws ASAPException {

        super();

        if(format == null || folderName == null || uri == null || senderE2E == null)
            throw new ASAPException("parameters must no be null");

        this.setAction(ASAPAndroid.ASAP_CHUNK_RECEIVED_ACTION);

        this.putExtra(ASAPServiceMethods.ERA_TAG, era);
        this.putExtra(ASAPServiceMethods.FORMAT_TAG, format);
        this.putExtra(ASAPAndroid.FOLDER, folderName);
        this.putExtra(ASAPServiceMethods.URI_TAG, uri);
        this.putExtra(ASAPAndroid.SENDER_E2E, senderE2E);
        this.putExtra(ASAPAndroid.SENDER_POINT2POINT, senderPoint2Point);
        this.putExtra(ASAPAndroid.VERIFIED, verified);
        this.putExtra(ASAPAndroid.ENCRYPTED, encrypted);
        this.putExtra(ASAPAndroid.CONNECTION_TYPE, connectionType);

        this.format = format;
        this.folder = folderName;
        this.uri = uri;
        this.era = era;
        this.senderE2E = senderE2E;
        this.senderPoint2Point = senderPoint2Point;
        this.verified = verified;
        this.encrypted = encrypted;
        this.connectionType = connectionType;
    }

    public ASAPChunkReceivedBroadcastIntent(Intent intent) throws ASAPException {
        super();

        // just parse extras
        this.format = intent.getStringExtra(ASAPServiceMethods.FORMAT_TAG);
        this.folder = intent.getStringExtra(ASAPAndroid.FOLDER);
        this.uri = intent.getStringExtra(ASAPServiceMethods.URI_TAG);
        this.era = intent.getIntExtra(ASAPServiceMethods.ERA_TAG, 0);
        this.senderE2E = intent.getStringExtra(ASAPAndroid.SENDER_E2E);
        this.senderPoint2Point = intent.getStringExtra(ASAPAndroid.SENDER_POINT2POINT);
        this.verified = intent.getBooleanExtra(ASAPAndroid.VERIFIED, false);
        this.encrypted = intent.getBooleanExtra(ASAPAndroid.ENCRYPTED, false);
        this.connectionType = null;
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

    public String getSenderPoint2Point() {
        return this.senderPoint2Point;
    }

    public boolean getVerified() {
        return this.verified;
    }

    public boolean getEncrypted() {
        return this.encrypted;
    }

    // TODO
    public EncounterConnectionType getConnectionType() {
        throw new SharkNotSupportedException("no implemented yet");
    }
}
