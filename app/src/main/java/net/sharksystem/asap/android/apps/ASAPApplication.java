package net.sharksystem.asap.android.apps;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import net.sharksystem.asap.ASAPChunkReceivedListener;
import net.sharksystem.asap.ASAPEngine;
import net.sharksystem.asap.ASAPEngineFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.ASAP;
import net.sharksystem.asap.android.ASAPChunkReceivedBroadcastIntent;
import net.sharksystem.asap.android.ASAPServiceCreationIntent;
import net.sharksystem.asap.android.Util;
import net.sharksystem.asap.apps.ASAPMessages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static android.support.v4.content.PermissionChecker.PERMISSION_DENIED;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;
import static net.sharksystem.asap.ASAPEngineFS.DEFAULT_ROOT_FOLDER_NAME;

public class ASAPApplication extends BroadcastReceiver {
    private static final int MY_ASK_FOR_PERMISSIONS_REQUEST = 100;
    private static ASAPApplication singleton;
    private CharSequence asapOwner;
    private CharSequence rootFolder;
    private boolean onlineExchange;
    private boolean initialized = false;

    private boolean btDisoverableOn = false;
    private boolean btDisoveryOn = false;
    private boolean btEnvironmentOn = false;

    private ASAPActivity activity;

    private List<String> requiredPermissions;
    private List<String> grantedPermissions = new ArrayList<>();
    private List<String> deniedPermissions = new ArrayList<>();
    private int activityCount = 0;
    private List<CharSequence> onlinePeerList = new ArrayList<>();

    /**
     * setup application by calling getASAPOwner(), getFolderName(), getASAPOnlineExchange().
     * Those messagen can and should be overwritten from actual implementations.
     */
    protected ASAPApplication() {
        this(ASAP.UNKNOWN_USER, DEFAULT_ROOT_FOLDER_NAME, ASAP.ONLINE_EXCHANGE_DEFAULT);
    }

    protected ASAPApplication(CharSequence asapOwner,
                              CharSequence rootFolder,
                              boolean onlineExchange) {
        this.asapOwner = asapOwner;
        this.rootFolder = rootFolder;
        this.onlineExchange = onlineExchange;
    }

    private void initialize() {
        if(!this.initialized) {
            Log.d(this.getLogStart(), "initialize ASAPService");

            // required permissions
            this.requiredPermissions = new ArrayList<>();
            this.requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            this.requiredPermissions.add(Manifest.permission.ACCESS_WIFI_STATE);
            this.requiredPermissions.add(Manifest.permission.CHANGE_WIFI_STATE);
            this.requiredPermissions.add(Manifest.permission.CHANGE_NETWORK_STATE);
            this.requiredPermissions.add(Manifest.permission.INTERNET);
            this.requiredPermissions.add(Manifest.permission.BLUETOOTH);
            this.requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            this.requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

            // check for write permissions
            this.askForPermissions();

            // start service - which allows service to outlive unbind
//            Intent asapServiceCreationIntent = new Intent(activity, ASAPService.class);
//            asapServiceCreationIntent.putExtra(ASAP.USER, this.asapOwner);

            // get owner when initializing
            this.asapOwner = this.getASAPOwner(this.getActivity());
            this.rootFolder = this.getASAPRootFolder(this.getActivity());
            this.onlineExchange = this.getASAPOnlineExchange(this.getActivity());

            try {
                Intent asapServiceCreationIntent = new ASAPServiceCreationIntent(activity,
                        this.asapOwner, this.rootFolder, this.onlineExchange);

                Log.d(this.getLogStart(), "start service with intent: "
                        + asapServiceCreationIntent.toString());

                this.activity.startService(asapServiceCreationIntent);
                this.initialized = true;
            } catch (ASAPException e) {
                Log.e(this.getLogStart(), "could not start ASAP Service - fatal");
            }
        }
    }

    /**
     * could be overwritten
     * @param activity
     * @return
     */
    protected boolean getASAPOnlineExchange(Activity activity) {
        return this.onlineExchange;
    }

    public boolean getASAPOnlineExchange() {
        return this.onlineExchange;
    }

    /**
     * could be overwritten
     */
    protected CharSequence getASAPRootFolder(Activity activity) {
        return this.rootFolder;
    }

    public CharSequence getASAPRootFolder() {
        return Util.getASAPRootDirectory(
                this.getActivity(), this.rootFolder, this.asapOwner).getAbsolutePath();
    }

    /**
     * could be overwritten
     */
    protected CharSequence getASAPOwner(Activity activity) {
        return this.asapOwner;
    }

    public CharSequence getASAPOwner() {
        return this.asapOwner;
    }

    public static ASAPApplication getASAPApplication() {
        if(ASAPApplication.singleton == null) {
            ASAPApplication.singleton = new ASAPApplication();
        }

        return ASAPApplication.singleton;
    }

    public String getApplicationRootFolder(String appName) {
        return this.getASAPRootFolder() + "/" + appName;
    }

    public void activityCreated(ASAPActivity asapActivity) {
        this.setActivity(asapActivity);
        this.initialize();

        this.activityCount++;
        Log.d(this.getLogStart(), "activity created. New activity count == "
                + this.activityCount);
    }

    public void activityDestroyed(ASAPActivity asapActivity) {
        this.activityCount--;
        Log.d(this.getLogStart(), "activity destroyed. New activity count == "
                + this.activityCount);
    }

    public ASAPActivity getActivity() {
        return this.activity;
    }

    void setActivity(ASAPActivity activity) {
        Log.d(this.getLogStart(), "activity set");
        this.activity = activity;
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }

    private void askForPermissions() {
        if(this.requiredPermissions.size() < 1) {
            Log.d(this.getLogStart(), "no further permissions to ask for");
            return;
        }

        String wantedPermission = requiredPermissions.remove(0);
        Log.d(this.getLogStart(), "handle permission " + wantedPermission);

        if (ContextCompat.checkSelfPermission(this.activity, wantedPermission)
                == PackageManager.PERMISSION_GRANTED) {
            // already granted
            Log.d(this.getLogStart(), wantedPermission + " already granted");
            this.grantedPermissions.add(wantedPermission);
            this.askForPermissions(); // next iteration
            return;
        }

        // not yet granted
        // ask for missing permission
        ActivityCompat.requestPermissions(this.activity, new String[]{wantedPermission},
                MY_ASK_FOR_PERMISSIONS_REQUEST);
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        // just notified - cannot do anything without that permission
        Log.d(this.getLogStart(), "got permission response for code == " + requestCode);
        if(requestCode != MY_ASK_FOR_PERMISSIONS_REQUEST) {
            Log.d(this.getLogStart(), "unknown request code - leave");
            return;
        }

        Log.d(this.getLogStart(), "permissions.length ==  " + permissions.length);
        Log.d(this.getLogStart(), "grantResults.length ==  " + grantResults.length);

        // should be only one permission.

        switch (grantResults[0]) {
            case PERMISSION_GRANTED:
                Log.d(this.getLogStart(), permissions[0] + ": granted");
                this.grantedPermissions.add(permissions[0]);
                break;
            case PERMISSION_DENIED:
                Log.d(this.getLogStart(), permissions[0] + ": denied");
                this. deniedPermissions.add(permissions[0]);
                break;
            default:
                Log.d(this.getLogStart(), permissions[0] + "unknown grant result - weired");
                break;
        }

        this.askForPermissions();
    }

    public void setBTDiscoverable(boolean on) {
        this.btDisoverableOn = on;
    }

    public void setBTEnvironmentRunning(boolean on) {
        this.btEnvironmentOn = on;

    }

    public void setBTDiscovery(boolean on) {
        this.btDisoveryOn = on;
    }

    public boolean getBTEnvironmentRunning() {
        return this.btEnvironmentOn;
    }

    public boolean getBTDiscoverable() {
        return this.btDisoverableOn;
    }

    public boolean getBTDiscovery() {
        return this.btDisoveryOn;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //                        asap received broadcast management                       //
    /////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(this.getLogStart(), "received asap chunk received from asap engine/service");

        try {
            ASAPChunkReceivedBroadcastIntent asapReceivedIntent
                    = new ASAPChunkReceivedBroadcastIntent(intent);

            // call listener - that's me in that case
            this.chunkReceived(
                    asapReceivedIntent.getFormat().toString(),
                    asapReceivedIntent.getUser().toString(),
                    asapReceivedIntent.getUri().toString(),
                    asapReceivedIntent.getFoldername().toString(),
                    asapReceivedIntent.getEra());

        } catch (ASAPException e) {
            Log.w(this.getLogStart(), "could not handle intent: " + e.getLocalizedMessage());

        }
    }

    private Map<CharSequence, ASAPChunkReceivedListener> chunkReceivedListener = new HashMap<>();

    private Map<CharSequence, Collection<ASAPMessageReceivedListener>> messageReceivedListener
            = new HashMap<>();

    public void chunkReceived(String format, String sender, String uri, String foldername, int era) {
        Log.d(this.getLogStart(), "got chunk received message: "
                + format + " | "+ sender + " | " + uri  + " | " + foldername + " | " + era);

        Log.d(this.getLogStart(), "going to inform listener about it");
        ASAPChunkReceivedListener listener = this.chunkReceivedListener.get(uri);
        if(listener != null) {
            listener.chunkReceived(format, sender, uri, era);
        }

        try {
            ASAPEngine existingASAPEngineFS = ASAPEngineFS.getExistingASAPEngineFS(foldername);
            ASAPMessages messages = existingASAPEngineFS.getChannel(uri).getMessages();

            Collection<ASAPMessageReceivedListener> messageListeners =
                    this.messageReceivedListener.get(uri);

            Log.d(this.getLogStart(), "going to inform message listener about it");
            if(messageListeners != null) {
                for(ASAPMessageReceivedListener messageListener : messageListeners) {
                    messageListener.asapMessagesReceived(messages);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ASAPException e) {
            e.printStackTrace();
        }
    }

    /**
     * @deprecated
     * @param uri
     * @param listener
     */
    public void addChunkReceivedListener(CharSequence uri, ASAPChunkReceivedListener listener) {
        this.chunkReceivedListener.put(uri, listener);
    }

    public void addASAPMessageReceivedListener(CharSequence uri,
                                               ASAPMessageReceivedListener listener) {
        Collection<ASAPMessageReceivedListener> messageListeners =
                this.messageReceivedListener.get(uri);

        if(messageListeners == null) {
            this.messageReceivedListener.put(uri, new HashSet());
            this.addASAPMessageReceivedListener(uri, listener);
        } else {
            messageListeners.add(listener);
        }
    }

    public void removeChunkReceivedListener(CharSequence uri) {
        this.chunkReceivedListener.remove(uri);
    }

    public List<CharSequence> getOnlinePeerList() {
        return this.onlinePeerList;
    }

    public void setOnlinePeersList(List<CharSequence> peerList) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("#online peers: ");
        sb.append(peerList.size());
        for(CharSequence peerName : peerList) {
            sb.append(" | ");
            sb.append(peerName);
        }

        Log.d(this.getLogStart(), sb.toString());
        if(onlinePeerList.size() < peerList.size()) {
            Toast.makeText(this.getActivity(),
                    "new online connections", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this.getActivity(),
                    "online connections changed", Toast.LENGTH_SHORT).show();
        }

        this.onlinePeerList = peerList;
    }
}
