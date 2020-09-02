package net.sharksystem.asap.android;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
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
                ASAPPeer.DEFAULT_MAX_PROCESSING_TIME);
    }

    public ASAPServiceCreationIntent(Activity activity, CharSequence owner, CharSequence rootFolder,
        boolean onlineExchange, Collection<CharSequence> supportedFormats, long maxExecutionTime)
            throws ASAPException {

        super(activity, ASAPService.class);

        if(owner == null || rootFolder == null)
            throw new ASAPException("parameters must no be null");

        this.owner = owner;
        this.rootFolder = rootFolder;
        this.onlineExchange = onlineExchange;
        this.maxExecutionTime = maxExecutionTime;

        this.putExtra(ASAPAndroid.USER, owner);
        this.putExtra(ASAPAndroid.FOLDER, rootFolder);
        this.putExtra(ASAPAndroid.ONLINE_EXCHANGE, onlineExchange);
        this.putExtra(ASAPAndroid.MAX_EXECUTION_TIME, maxExecutionTime);

        if(supportedFormats != null) {
            this.supportFormatsList = new ArrayList<>();
            for (CharSequence supportedFormat : supportedFormats) {
                supportFormatsList.add(supportedFormat);
            }
            this.putCharSequenceArrayListExtra(ASAPAndroid.SUPPORTED_FORMATS, supportFormatsList);
        } else {
            Log.d(this.getLogStart(), "no format set - FAILURE?");
        }
    }

    public ASAPServiceCreationIntent(Intent intent) {
        super();

        // just parse extras
        this.owner = intent.getStringExtra(ASAPAndroid.USER);
        this.rootFolder = intent.getStringExtra(ASAPAndroid.FOLDER);
        this.onlineExchange = intent.getBooleanExtra(ASAPAndroid.ONLINE_EXCHANGE,
                ASAPAndroid.ONLINE_EXCHANGE_DEFAULT);
        this.maxExecutionTime = intent.getLongExtra(ASAPAndroid.MAX_EXECUTION_TIME,
                ASAPPeer.DEFAULT_MAX_PROCESSING_TIME);
        this.supportFormatsList = intent.getCharSequenceArrayListExtra(ASAPAndroid.SUPPORTED_FORMATS);
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

    private String getLogStart() {
        return net.sharksystem.asap.util.Log.startLog(this).toString();
    }
}
