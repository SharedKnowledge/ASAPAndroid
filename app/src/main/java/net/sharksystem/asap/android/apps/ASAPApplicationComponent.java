package net.sharksystem.asap.android.apps;

import android.content.Context;

import net.sharksystem.asap.ASAPException;

public interface ASAPApplicationComponent {
    Context getContext() throws ASAPException;
    ASAPAndroidPeer getASAPApplication();
}
