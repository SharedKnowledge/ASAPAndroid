package net.sharksystem.asap.android.apps;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.CallSuper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import net.sharksystem.Utils;
import net.sharksystem.asap.ASAPEngineFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.asap.android.ASAPAndroid;
import net.sharksystem.asap.android.ASAPChunkReceivedBroadcastIntent;
import net.sharksystem.asap.android.ASAPServiceCreationIntent;
import net.sharksystem.asap.android.Util;
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
    private Collection<CharSequence> supportedFormats;
    private CharSequence asapOwner = null;
    private CharSequence rootFolder;
    private boolean onlineExchange;
    private boolean initialized = false;

    private boolean btDisoverableOn = false;
    private boolean btDisoveryOn = false;
    private boolean btEnvironmentOn = false;

    private Activity activity;

    private List<String> requiredPermissions;
    private List<String> grantedPermissions = new ArrayList<>();
    private List<String> deniedPermissions = new ArrayList<>();
    private int activityASAPActivities = 0;
    private List<CharSequence> onlinePeerList = new ArrayList<>();

    /**
     * Setup application using default setting
     * @param supportedFormats ensure that asap engines using that formats are present -
     *                         create if necessary.
     */
    protected ASAPApplication(Collection<CharSequence> supportedFormats, Activity initialActivity) {
        this(supportedFormats, ASAPAndroid.UNKNOWN_USER, DEFAULT_ROOT_FOLDER_NAME,
                ASAPAndroid.ONLINE_EXCHANGE_DEFAULT, initialActivity);
    }

    int getNumberASAPActivities() {
        return this.activityASAPActivities;
    }

    /**
     * setup application without parameter. Use default for owner, root folder for asap storage
     * and online exchange behaviour. Don't setup any asap engine - take engines which are
     * already present when starting up.
    protected ASAPApplication(Activity initialActivity) {
        this(null, ASAPAndroid.UNKNOWN_USER, DEFAULT_ROOT_FOLDER_NAME,
                ASAPAndroid.ONLINE_EXCHANGE_DEFAULT);
    }
     */

    protected ASAPApplication(Collection<CharSequence> supportedFormats,
                CharSequence asapOwner,
                CharSequence rootFolder,
                boolean onlineExchange,
                Activity initialActivity) {

        this.supportedFormats = supportedFormats;
        this.asapOwner = asapOwner;
        this.rootFolder = rootFolder;
        this.onlineExchange = onlineExchange;

        // set context
        this.setActivity(initialActivity);

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
        Log.d(this.getLogStart(), "ask for required permissions");
        this.askForPermissions();
    }

    /**
     * This method must be called launch the ASAP application. The ASAP service is started that
     * deals with all supported layer 2 protocol and initiates ASAP session. Objects of
     * ASAPApplications (and derived classes) are in most cases proxies of this service.
     * <b>Never forget to launch your application.</b>
     */
    @CallSuper
    public void startASAPApplication() {
        if(!this.initialized) {
            Log.d(this.getLogStart(), "initialize and launch ASAP Service");
            // collect parameters
            if(this.asapOwner == null || this.asapOwner.equals(ASAPAndroid.UNKNOWN_USER)) {
                Log.d(this.getLogStart(), "asapOwnerID not set at all or set to default - call getASAPOwnerID");
                this.asapOwner = this.getOwnerID();
            } else {
                Log.d(this.getLogStart(), "owner already set");
            }

            if(this.supportedFormats == null || this.supportedFormats.size() < 1) {
                Log.d(this.getLogStart(), "supportedFormats null or empty - call getSupportedFormats()");
                this.supportedFormats = this.getSupportFormats();
            } else {
                Log.d(this.getLogStart(), "supportedFormats already set");
            }

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
        } else {
            Log.e(this.getLogStart(), "try to re-start application - not allowed. Ignored");
        }
    }

    /**
     * ASAP application and service run separately. Nevertheless, ASAP application knows the
     * location where each ASAP storage is kept. This method provides an object reference. Handle
     * this method with great care. You would work on a storage in parallel with the ASAP service.
     * There can be race conditions and most probably synchronization issues. It be also a
     * performance booster. You should really know what you are doing.
     *
     * @param appFormat
     * @return
     * @throws IOException
     * @throws ASAPException
     */
    public ASAPStorage getASAPStorage(CharSequence appFormat) throws IOException, ASAPException {
        Log.d(this.getLogStart(), "convention: asap storage are in a folder ownerName/formatName");

        String rootFolder = this.getApplicationRootFolder(appFormat.toString());
        Log.d(this.getLogStart(), "use rootFolder: " + rootFolder);

        return ASAPEngineFS.getASAPStorage(
                this.asapOwner.toString(),
                rootFolder,
                appFormat.toString());
    }

    /**
     * ASAP engines exchange message during an ASAP session which are already kept in their storage.
     * <i>online exchange</i> denotes the feature that message are sent which are produced during
     * a running asap session. That's most useful for any kind of chat application.
     * @return status of online exchange.
     */
    public boolean getASAPOnlineExchange() {
        return this.onlineExchange;
    }

    /**
     * could be overwritten
     */
    protected CharSequence getASAPRootFolder(Activity activity) {
        return this.rootFolder;
    }

    /**
     *
     * @return root folder of all information stored by the asap service. Handle this information
     * with great care.
     */
    public CharSequence getASAPRootFolder() {
        return Util.getASAPRootDirectory(
                this.getActivity(), this.rootFolder, this.getOwnerID()).getAbsolutePath();
    }

    public CharSequence getASAPComponentFolder(CharSequence format) {
        return this.getASAPRootFolder() +"/" + format;
    }

    /**
     * @return asap owner id - if set
     */
    public CharSequence getOwnerID() {
        return this.asapOwner;
    }

    /**
     * @return asap owner name - if set
     */
    public CharSequence getOwnerName() {
        return ASAPEngineFS.ANONYMOUS_OWNER;
    }

    /**
     *
     * @return list of supported formats supported by this asap peer / service. You have defined
     * those formats during object initialization.
     */
    public Collection<CharSequence> getSupportFormats() {
        return this.supportedFormats;
    }

    public static ASAPApplication getASAPApplication() {
        if(ASAPApplication.singleton == null) {
            throw new ASAPComponentNotYetInitializedException("ASAP Application not yet initialized");
        }

        return ASAPApplication.singleton;
    }

    /**
     * Factory method: Setup an asap application. See documentation in the wiki. Don't forget
     * to launch you application by calling startApplication afterwards.
     * @param supportedFormats
     * @param initialActivity
     * @return
     */
    public static ASAPApplication initializeASAPApplication(
            Collection<CharSequence> supportedFormats, Activity initialActivity) {
        if(ASAPApplication.singleton == null) {
            ASAPApplication.singleton = new ASAPApplication(supportedFormats, initialActivity);
        } else {
            Log.e(ASAPApplication.class.getSimpleName(),
                    "tried to initialized already initialized application - ignored.");
        }

        return ASAPApplication.singleton;
    }

    /**
     * Factory method: Setup an asap application. See documentation in the wiki. Don't forget
     * to launch you application by calling startApplication afterwards.
     * @param one supported format
     * @param initialActivity
     * @return
     */
    public static ASAPApplication initializeASAPApplication(
            CharSequence supportedFormat, Activity initialActivity) {
        Collection<CharSequence> formats = new HashSet<>();
        return ASAPApplication.initializeASAPApplication(formats, initialActivity);
    }

    /**
     *
     * @param appName
     * @return root folder of information kept by an asap engine. Better not change those
     * information.
     */
    public String getApplicationRootFolder(String appName) {
        appName = Utils.url2FileName(appName);
        String absoluteASAPApplicationRootFolder = this.getASAPRootFolder() + "/" + appName;
        Log.d(this.getLogStart(), "absolute asap app rootfolder: "
                + absoluteASAPApplicationRootFolder);

        return absoluteASAPApplicationRootFolder;
    }

    @CallSuper
    public void activityCreated(ASAPActivity asapActivity) {
        this.setActivity(asapActivity);
        /* was to tricky barely understandable
        if(initASAPApplication) this.startASAPApplication();
         */

        this.activityASAPActivities++;
        Log.d(this.getLogStart(), "activity created. New activity count == "
                + this.activityASAPActivities);
    }

    @CallSuper
    public void activityDestroyed(ASAPActivity asapActivity) {
        this.activityASAPActivities--;
        Log.d(this.getLogStart(), "activity destroyed. New activity count == "
                + this.activityASAPActivities);
    }

    public Activity getActivity() {
        return this.activity;
    }

    void setActivity(Activity activity) {
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

        if (ContextCompat.checkSelfPermission(this.getActivity(), wantedPermission)
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

    private Map<CharSequence, Collection<ASAPUriContentChangedListener>> uriContentChangedListener
            = new HashMap<>();

    public void chunkReceived(String format, String sender, String uri, String foldername, int era) {
        Log.d(this.getLogStart(), "got chunkReceived message: "
                + format + " | "+ sender + " | " + uri  + " | " + foldername + " | " + era);

        // inform uri changed listener - if any
        Collection<ASAPUriContentChangedListener> uriListeners =
                this.uriContentChangedListener.get(format);

        Log.d(this.getLogStart(), "going to inform uri changed listener about it: "
                + uriListeners);

        if(uriListeners != null) {
            for(ASAPUriContentChangedListener uriListener : uriListeners) {
                uriListener.asapUriContentChanged(uri);
            }
        }

        // inform message listeners - if any
        Collection<ASAPMessageReceivedListener> messageListeners =
                this.messageReceivedListener.get(format);

        Log.d(this.getLogStart(), "going to inform message listener about it: "
                + messageListeners);

        if(messageListeners != null) {
            ASAPMessages asapMessages = Helper.getMessagesByChunkReceivedInfos(
                    format, sender, uri, foldername, era);

            if(asapMessages == null) {
                Log.e(this.getLogStart(), "cannot create message - failure - give up");
                return;
            }

            for(ASAPMessageReceivedListener messageListener : messageListeners) {
                messageListener.asapMessagesReceived(asapMessages);
            }
        }
    }

    public final void addASAPUriContentChangedListener(
            CharSequence format, ASAPUriContentChangedListener listener) {
        Log.d(this.getLogStart(), "going to add asap uri changed listener for " + format);
        Collection<ASAPUriContentChangedListener> uriChangedListeners =
                this.uriContentChangedListener.get(format);

        if(uriChangedListeners == null) {
            uriChangedListeners = new HashSet();
            this.uriContentChangedListener.put(format, uriChangedListeners);
        }

        uriChangedListeners.add(listener);
    }

    public final void removeASAPUriContentChangedListener(
            CharSequence format, ASAPUriContentChangedListener listener) {
        Collection<ASAPUriContentChangedListener> uriChangedListeners =
                this.uriContentChangedListener.get(format);

        Log.d(this.getLogStart(), "going to remove asap uri changed listener for " + format);

        if(uriChangedListeners != null) {
            uriChangedListeners.remove(listener);
        }
    }

    /**
     * Subscribe to get notified about incoming asap message of a given format/application
     * @param format
     * @param listener
     */
    public final void addASAPMessageReceivedListener(CharSequence format,
                                       ASAPMessageReceivedListener listener) {
        Log.d(this.getLogStart(), "going to add asap message receiver for " + format);
        Collection<ASAPMessageReceivedListener> messageListeners =
                this.messageReceivedListener.get(format);

        if(messageListeners == null) {
            messageListeners = new HashSet();
            this.messageReceivedListener.put(format, messageListeners);
        }

        messageListeners.add(listener);
    }

    public final void removeASAPMessageReceivedListener(CharSequence format,
                                               ASAPMessageReceivedListener listener) {
        Collection<ASAPMessageReceivedListener> messageListeners =
                this.messageReceivedListener.get(format);

        Log.d(this.getLogStart(), "going to remove asap message receiver for " + format);

        if(messageListeners != null) {
            messageListeners.remove(listener);
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

    public void setupDrawerLayout(Activity activity) {
        Log.d(this.getLogStart(), "setupDrawerLayout dummy called: could be overwritten if needed. Don't do anything here");
    }

    private String getLogStart() {
//        int objectID = this.hashCode();
//        return "ASAPApplication(" + objectID + ")";
        return "ASAPApplication";
    }
}
