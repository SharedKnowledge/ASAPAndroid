package net.sharksystem.asap.android;

import android.app.Activity;
import android.content.Intent;

import net.sharksystem.asap.ASAPEngine;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.MultiASAPEngineFS;
import net.sharksystem.asap.android.service.ASAPService;

public class ASAPServiceCreationIntent extends Intent {

    private final CharSequence owner;
    private final CharSequence rootFolder;
    private final boolean onlineExchange;
    private final long maxExecutionTime;

    public ASAPServiceCreationIntent(Activity activity, CharSequence owner, CharSequence rootFolder,
                                     boolean onlineExchange)
            throws ASAPException {

        this(activity, owner, rootFolder, onlineExchange,
                MultiASAPEngineFS.DEFAULT_MAX_PROCESSING_TIME);
    }

    public ASAPServiceCreationIntent(Activity activity, CharSequence owner, CharSequence rootFolder,
        boolean onlineExchange, long maxExecutionTime)
            throws ASAPException {

        super(activity, ASAPService.class);

        if(owner == null || rootFolder == null)
            throw new ASAPException("parameters must no be null");

        this.putExtra(ASAP.USER, owner);
        this.putExtra(ASAP.FOLDER, rootFolder);
        this.putExtra(ASAP.ONLINE_EXCHANGE, onlineExchange);
        this.putExtra(ASAP.MAX_EXECUTION_TIME, maxExecutionTime);

        this.owner = owner;
        this.rootFolder = rootFolder;
        this.onlineExchange = onlineExchange;
        this.maxExecutionTime = maxExecutionTime;
    }

    public ASAPServiceCreationIntent(Intent intent) {
        super();

        // just parse extras
        this.owner = intent.getStringExtra(ASAP.USER);
        this.rootFolder = intent.getStringExtra(ASAP.FOLDER);
        this.onlineExchange = intent.getBooleanExtra(ASAP.ONLINE_EXCHANGE,
                ASAP.ONLINE_EXCHANGE_DEFAULT);
        this.maxExecutionTime = intent.getLongExtra(ASAP.MAX_EXECUTION_TIME,
                MultiASAPEngineFS.DEFAULT_MAX_PROCESSING_TIME);

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

    public long getMaxExecutionTime() {
        return this.maxExecutionTime;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("owner: ");
        sb.append(this.owner);
        sb.append(" | folder: ");
        sb.append(this.rootFolder);
        sb.append(" | onlineExchange: ");
        sb.append(this.onlineExchange);
        sb.append(" | maxExecutionTime: ");
        sb.append(this.maxExecutionTime);

        return sb.toString();
    }

}
