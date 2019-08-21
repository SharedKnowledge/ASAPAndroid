package net.sharksystem.asap.android;

import android.content.Context;

public abstract class MacLayerEngine {
    private final ASAPService ASAPService;
    private final Context context;

    public MacLayerEngine(ASAPService ASAPService, Context context) {
        this.ASAPService = ASAPService;
        this.context = context;
    }

    protected Context getContext() {
        return this.context;
    }

    protected ASAPService getASAPService() {
        return this.ASAPService;
    }

    public abstract void start();
    public abstract void stop();

    public void restart() {
        this.stop();
        this.start();
    }
}
