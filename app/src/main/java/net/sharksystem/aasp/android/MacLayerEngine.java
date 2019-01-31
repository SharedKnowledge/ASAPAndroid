package net.sharksystem.aasp.android;

import android.content.Context;

public abstract class MacLayerEngine {
    private final AASPService aaspService;
    private final Context context;

    public MacLayerEngine(AASPService aaspService, Context context) {
        this.aaspService = aaspService;
        this.context = context;
    }

    protected Context getContext() {
        return this.context;
    }

    protected AASPService getAASPService() {
        return this.aaspService;
    }

    public abstract void start();
    public abstract void stop();

    public void restart() {
        this.stop();
        this.start();
    }
}
