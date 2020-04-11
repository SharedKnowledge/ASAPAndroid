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

import net.sharksystem.asap.ASAPEngineFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.asap.android.ASAPAndroid;
import net.sharksystem.asap.android.ASAPChunkReceivedBroadcastIntent;
import net.sharksystem.asap.android.ASAPServiceCreationIntent;
import net.sharksystem.asap.android.Util;
import net.sharksystem.asap.apps.ASAPMessages;
import net.sharksystem.asap.util.Helper;

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
    private final Collection<CharSequence> supportedFormats;
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
     * Setup application using default setting
     * @param supportedFormats ensure that asap engines using that formats are present -
     *                         create if necessary.
     */
    protected ASAPApplication(Collection<CharSequence> supportedFormats) {
        this(supportedFormats, ASAPAndroid.UNKNOWN_USER, DEFAULT_ROOT_FOLDER_NAME,
                ASAPAndroid.ONLINE_EXCHANGE_DEFAULT);
    }

    /**
     * setup application without parameter. Use default for owner, root folder for asap storage
     * and online exchange behaviour. Don't setup any asap engine - take engines which are
     * already present when starting up.
     */
    protected ASAPApplication() {
        this(null, ASAPAndroid.UNKNOWN_USER, DEFAULT_ROOT_FOLDER_NAME,
                ASAPAndroid.ONLINE_EXCHANGE_DEFAULT);
    }

    protected ASAPApplication(Collection<CharSequence> supportedFormats,
                CharSequence asapOwner,
                CharSequence rootFolder,
                boolean onlineExchange) {

        this.supportedFormats = supportedFormats;
        this.asapOwner = asapOwner;
        this.rootFolder = rootFolder;
        this.onlineExchange = onlineExchange;
    }

    private void initialize() {
        if(!this.initialized) {
            Log.d(this.getLogStart(), "initialize ASAP Application user side");

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

            // get owner when initializing
            this.asapOwner = this.getASAPOwnerID();
            //this.rootFolder = this.getASAPRootFolder(); // service creates absolute path
            this.onlineExchange = this.getASAPOnlineExchange();

            try {
                Intent asapServiceCreationIntent = new ASAPServiceCreationIntent(activity,
                        this.asapOwner, this.rootFolder, this.onlineExchange, this.supportedFormats);

                Log.d(this.getLogStart(), "start service with intent: "
                        + asapServiceCreationIntent.toString());

                this.activity.startService(asapServiceCreationIntent);
                this.initialized = true;
            } catch (ASAPException e) {
                Log.e(this.getLogStart(), "could not start ASAP Service - fatal");
            }
        }
    }

    public ASAPStorage getASAPStorage(CharSequence appFormat) throws IOException, ASAPException {
        return ASAPEngineFS.getASAPStorage(
                this.asapOwner.toString(),
                this.getApplicationRootFolder(appFormat.toString()),
                appFormat.toString());
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
    public CharSequence getASAPOwnerID() {
        return this.asapOwner;
    }

    public Collection<CharSequence> getSupportFormats() {
        return this.supportedFormats;
    }

    public static ASAPApplication getASAPApplication() {
        if(ASAPApplication.singleton == null) {
            ASAPApplication.singleton = new ASAPApplication();
        }

        return ASAPApplication.singleton;
    }

    protected void setASAPApplication(ASAPApplication asapApplication) {
        ASAPApplication.singleton = asapApplication;
    }

    public static ASAPApplication getASAPApplication(Collection<CharSequence> supportedFormats) {
        if(ASAPApplication.singleton == null) {
            ASAPApplication.singleton = new ASAPApplication(supportedFormats);
        }

        return ASAPApplication.singleton;
    }

    public static ASAPApplication getASAPApplication(CharSequence supportedFormat) {
        if(ASAPApplication.singleton == null) {
            Collection<CharSequence> formats = new HashSet<>();
            formats.add(supportedFormat);
            ASAPApplication.singleton = new ASAPApplication(formats);
        }

        return ASAPApplication.singleton;
    }

    public String getApplicationRootFolder(String appName) {
        return this.getASAPRootFolder() + "/" + appName;
    }

    public void activityCreated(ASAPActivity asapActivity, boolean initASAPApplication) {
        this.setActivity(asapActivity);
        if(initASAPApplication) this.initialize();

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

    private Map<CharSequence, Collection<ASAPMessageReceivedListener>> messageReceivedListener
            = new HashMap<>();

    private Map<CharSequence, Collection<ASAPUriContentChangedListener>> uriChangedListener
            = new HashMap<>();

    public void chunkReceived(String format, String sender, String uri, String foldername, int era) {
        Log.d(this.getLogStart(), "got chunkReceived message: "
                + format + " | "+ sender + " | " + uri  + " | " + foldername + " | " + era);

        // inform uri changed listener
        Collection<ASAPUriContentChangedListener> uriListeners =
                this.uriChangedListener.get(uri);

        Log.d(this.getLogStart(), "going to inform uri changed listener about it: "
                + uriListeners);

        if(uriListeners != null) {
            for(ASAPUriContentChangedListener uriListener : uriListeners) {
                uriListener.asapUriContentChanged(uri);
            }
        }

        ASAPMessages asapMessages = Helper.getMessageByChunkReceivedInfos(
                format, sender, uri, foldername, era);

        if(asapMessages == null) {
            Log.e(this.getLogStart(), "cannot create message - failure - give up");
            return;
        }

        Collection<ASAPMessageReceivedListener> messageListeners =
                this.messageReceivedListener.get(uri);

        Log.d(this.getLogStart(), "going to inform message listener about it: "
                + messageListeners);

        if(messageListeners != null) {
            for(ASAPMessageReceivedListener messageListener : messageListeners) {
                messageListener.asapMessagesReceived(asapMessages);
            }
        }

    }

    /**
     * Subscribe to get notified about incoming asap message of a given format/application
     * @param format
     * @param listener
     */
    public void addASAPMessageReceivedListener(CharSequence format,
                                       ASAPMessageReceivedListener listener) {

        Log.d(this.getLogStart(), "going to add asap message receiver for " + format);
        Collection<ASAPMessageReceivedListener> messageListeners =
                this.messageReceivedListener.get(format);

        if(messageListeners == null) {
            this.messageReceivedListener.put(format, new HashSet());
            this.addASAPMessageReceivedListener(format, listener);
        } else {
            messageListeners.add(listener);
        }
    }

    public void removeASAPMessageReceivedListener(CharSequence format,
                                               ASAPMessageReceivedListener listener) {
        Collection<ASAPMessageReceivedListener> messageListeners =
                this.messageReceivedListener.get(format);

        Log.d(this.getLogStart(), "going to remove asap message receiver for " + format);

        if(messageListeners != null) {
            messageListeners.remove(listener);
        }
    }

    public void addASAPUriContentChangedListener(CharSequence format,
                                                 ASAPUriContentChangedListener listener) {

        Collection<ASAPUriContentChangedListener> uriListeners =
                this.uriChangedListener.get(format);

        if(uriListeners == null) {
            this.uriChangedListener.put(format, new HashSet());
            this.addASAPUriContentChangedListener(format, listener);
        } else {
            uriListeners.add(listener);
        }
    }

    public void removeASAPUriContentChangedListener(CharSequence format,
                                                 ASAPUriContentChangedListener listener) {
        Collection<ASAPUriContentChangedListener> uriListeners =
                this.uriChangedListener.get(format);

        if(uriListeners != null) {
            uriListeners.remove(listener);
        }
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

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }
}
