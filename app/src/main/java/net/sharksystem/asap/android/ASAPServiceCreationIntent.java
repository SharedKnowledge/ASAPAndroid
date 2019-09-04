package net.sharksystem.asap.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.service.ASAPService;

public class ASAPServiceCreationIntent extends Intent {

    private final CharSequence owner;
    private final CharSequence rootFolder;
    private final boolean onlineExchange;

    public ASAPServiceCreationIntent(Activity activity, CharSequence owner, CharSequence rootFolder,
                                     boolean onlineExchange) throws ASAPException {

        super(activity, ASAPService.class);

        if(owner == null || rootFolder == null)
            throw new ASAPException("parameters must no be null");

        this.putExtra(ASAP.USER, owner);
        this.putExtra(ASAP.FOLDER, rootFolder);
        this.putExtra(ASAP.ONLINE_EXCHANGE, onlineExchange);

        this.owner = owner;
        this.rootFolder = rootFolder;
        this.onlineExchange = onlineExchange;
    }

    public ASAPServiceCreationIntent(Intent intent) {
        super();

        // just parse extras
        this.owner = intent.getStringExtra(ASAP.USER);
        this.rootFolder = intent.getStringExtra(ASAP.FOLDER);
        this.onlineExchange = intent.getBooleanExtra(ASAP.ONLINE_EXCHANGE,
                ASAP.ONLINE_EXCHANGE_DEFAULT);

    }

    public CharSequence getOwner() {
        return this.owner;
    }

    public CharSequence getRootFolder() {
        return this.rootFolder;
    }

    public boolean isOnlineExchange() {
        return this.onlineExchange;
    }
}
