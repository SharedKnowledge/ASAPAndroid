package net.sharksystem.asap.android;

import android.app.Activity;
import android.content.Intent;

import net.sharksystem.asap.ASAPEngine;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.MultiASAPEngineFS;
import net.sharksystem.asap.android.service.ASAPService;

import java.util.ArrayList;
import java.util.Collection;

public class ASAPServiceCreationIntent extends Intent {
    private final CharSequence owner;
    private final CharSequence rootFolder;
    private final boolean onlineExchange;
    private final long maxExecutionTime;
    private ArrayList<CharSequence> supportFormatsList;

    public ASAPServiceCreationIntent(Activity activity, CharSequence owner, CharSequence rootFolder,
                                     boolean onlineExchange,
                                     Collection<CharSequence> supportedFormats)
            throws ASAPException {

        this(activity, owner, rootFolder, onlineExchange, supportedFormats,
                MultiASAPEngineFS.DEFAULT_MAX_PROCESSING_TIME);
    }

    public ASAPServiceCreationIntent(Activity activity, CharSequence owner, CharSequence rootFolder,
        boolean onlineExchange, Collection<CharSequence> supportedFormats, long maxExecutionTime)
            throws ASAPException {

        super(activity, ASAPService.class);

        if(owner == null || rootFolder == null)
            throw new ASAPException("parameters must no be null");

        this.putExtra(ASAP.USER, owner);
        this.putExtra(ASAP.FOLDER, rootFolder);
        this.putExtra(ASAP.ONLINE_EXCHANGE, onlineExchange);
        this.putExtra(ASAP.MAX_EXECUTION_TIME, maxExecutionTime);

        ArrayList<CharSequence> supportFormatsList = new ArrayList<>();
        for(CharSequence supportedFormat : supportedFormats) {
            supportFormatsList.add(supportedFormat);
        }
        this.putCharSequenceArrayListExtra(ASAP.SUPPORTED_FORMATS, supportFormatsList);


        this.owner = owner;
        this.rootFolder = rootFolder;
        this.onlineExchange = onlineExchange;
        this.maxExecutionTime = maxExecutionTime;
        this.supportFormatsList = supportFormatsList;
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
        this.supportFormatsList = intent.getCharSequenceArrayListExtra(ASAP.SUPPORTED_FORMATS);
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

    public ArrayList getSupportedFormats() {
        return this.supportFormatsList;
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
        sb.append(" | supportedFormats: ");
        sb.append(this.supportFormatsList);

        return sb.toString();
    }
}
