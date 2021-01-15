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
import net.sharksystem.asap.ASAPEnvironmentChangesListener;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.asap.android.ASAPChunkReceivedBroadcastIntent;
import net.sharksystem.asap.android.ASAPServiceCreationIntent;
import net.sharksystem.asap.android.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.support.v4.content.PermissionChecker.PERMISSION_DENIED;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class ASAPAndroidPeer extends BroadcastReceiver implements ASAPPeer {
    private static final int MY_ASK_FOR_PERMISSIONS_REQUEST = 100;
    private static ASAPAndroidPeer singleton;
    private ASAPPeerFS asapPeerApplicationSide = null;
    private Collection<CharSequence> supportedFormats;
    private CharSequence asapOwner = null;
    private CharSequence rootFolder;
    private boolean onlineExchange;
    private boolean started = false;

    private boolean btDisoverableOn = false;
    private boolean btDisoveryOn = false;
    private boolean btEnvironmentOn = false;

    private Activity activity;

    private List<String> requiredPermissions;
    private List<String> grantedPermissions = new ArrayList<>();
    private List<String> deniedPermissions = new ArrayList<>();
    private int activityASAPActivities = 0;

    //////////////////////////////////////////////////////////////////////////////////////////////
    //                                     construction                                         //
    //////////////////////////////////////////////////////////////////////////////////////////////

    static ASAPAndroidPeer getASAPAndroidPeer() {
        if(ASAPAndroidPeer.singleton == null) {
            throw new ASAPComponentNotYetInitializedException(
                    "ASAPAndroidPeer not yet initialized");
        }

        return ASAPAndroidPeer.singleton;
    }

    public static boolean peerInitialized() {
        return ASAPAndroidPeer.singleton != null;
    }

    /**
     * Initialize the ASAP Peer (application) side. Do not forget to *start* this peer to launch
     * the ASAP service and set anything in motion.
     * @return
     */
    public static void initializePeer(
            Collection<CharSequence> supportedFormats, Activity initialActivity)
            throws IOException, ASAPException {

        new ASAPAndroidPeer(supportedFormats, ASAPPeer.UNKNOWN_USER,
                ASAPPeerFS.DEFAULT_ROOT_FOLDER_NAME,
                ASAPPeer.ONLINE_EXCHANGE_DEFAULT, initialActivity);

    }

    public static void initializePeer(CharSequence asapOwner,
                                      Collection<CharSequence> supportedFormats,
                                      Activity initialActivity)
            throws IOException, ASAPException {

        new ASAPAndroidPeer(supportedFormats, asapOwner,
                ASAPPeerFS.DEFAULT_ROOT_FOLDER_NAME,
                ASAPPeer.ONLINE_EXCHANGE_DEFAULT, initialActivity);
    }

    public static void initializePeer(CharSequence asapOwner,
                                      Collection<CharSequence> supportedFormats,
                                      CharSequence rootFolder,
                                      Activity initialActivity)
            throws IOException, ASAPException {

        new ASAPAndroidPeer(supportedFormats, asapOwner, rootFolder,
                ASAPPeer.ONLINE_EXCHANGE_DEFAULT, initialActivity);
    }

    /**
     * Do not forget to call start() to actually launch peer, especially the service
     * @param supportedFormats
     * @param asapOwner
     * @param rootFolder
     * @param onlineExchange
     * @param initialActivity
     */
    private ASAPAndroidPeer(Collection<CharSequence> supportedFormats,
                              CharSequence asapOwner,
                              CharSequence rootFolder,
                              boolean onlineExchange,
                              Activity initialActivity) throws IOException, ASAPException {

        this.supportedFormats = supportedFormats;
        this.asapOwner = asapOwner;
        this.rootFolder = rootFolder;
        this.onlineExchange = onlineExchange;

        if(ASAPAndroidPeer.peerInitialized()) {
            throw new ASAPException("ASAPAndroidPeer already initialized - singleton already created");
        }

        // remember me
        ASAPAndroidPeer.singleton = this;

        // create proxy
        this.asapPeerApplicationSide =
                new ASAPPeerFS(asapOwner, this.getASAPRootFolder(), supportedFormats);

        // set context
        this.setActivity(initialActivity);

        Log.d(this.getLogStart(), "initialize ASAP Peer application side");
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
     * Start initialized ASAPAndroidPeer
     */
    public static void startPeer() {
        ASAPAndroidPeer.getASAPAndroidPeer().start();
    }

    public static boolean peerStarted() {
        return ASAPAndroidPeer.getASAPAndroidPeer().started;
    }

    /**
     * This method must be called launch the ASAP application. The ASAP service is started that
     * deals with all supported layer 2 protocol and initiates ASAP session. Objects of
     * ASAPApplications (and derived classes) are in most cases proxies of this service.
     * <b>Never forget to launch your application.</b>
     */
    @CallSuper
    public void start() {
        if(!this.started) {
            Log.d(this.getLogStart(), "initialize and launch ASAP Service");
            // collect parameters
            if(this.asapOwner == null || this.asapOwner.equals(ASAPPeer.UNKNOWN_USER)) {
                Log.d(this.getLogStart(),
                        "asapOwnerID not set at all or set to default - call getASAPOwnerID");
                this.asapOwner = this.getOwnerID();
            } else {
                Log.d(this.getLogStart(), "owner already set");
            }

            if(this.supportedFormats == null || this.supportedFormats.size() < 1) {
                Log.d(this.getLogStart(),
                        "supportedFormats null or empty - call getSupportedFormats()");
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
                this.started = true;
            } catch (ASAPException e) {
                Log.e(this.getLogStart(), "could not start ASAP Service - fatal");
            }
        } else {
            Log.e(this.getLogStart(), "try to re-start application - not allowed. Ignored");
        }
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

    private ASAPPeerFS getASAPPeerApplicationSide() {
        if(this.asapPeerApplicationSide == null) {
            // create application side proxy
            throw new ASAPComponentNotYetInitializedException("peer app side not initialized - that's a bug. Check peer constructor on application side");
        }

        return this.asapPeerApplicationSide;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                        getter                                             //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    int getNumberASAPActivities() {
        return this.activityASAPActivities;
    }

    /**
     *
     * @return list of supported formats supported by this asap peer / service. You have defined
     * those formats during object initialization.
     */
    public Collection<CharSequence> getSupportFormats() {
        return this.supportedFormats;
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
    public ASAPStorage getASAPStorage(CharSequence appFormat) throws IOException, ASAPException {
        Log.d(this.getLogStart(), "convention: asap storage are in a folder ownerName/formatName");

        String rootFolder = this.getApplicationRootFolder(appFormat.toString());
        Log.d(this.getLogStart(), "use rootFolder: " + rootFolder);

        return ASAPEngineFS.getASAPStorage(
                this.asapOwner.toString(),
                rootFolder,
                appFormat.toString());
    }
     */

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
    protected CharSequence getASAPRootFolder(Activity activity) {
        return this.rootFolder;
    }
     */

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
    public CharSequence getOwnerName() {
        return ASAPEngineFS.ANONYMOUS_OWNER;
    }
     */

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

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                              activity / status observation                                //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @CallSuper
    public void activityCreated(ASAPActivity asapActivity) {
        this.setActivity(asapActivity);
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

    public void notifyBTDiscoverable(boolean on) {
        this.btDisoverableOn = on;
    }

    public void notifyBTEnvironmentRunning(boolean on) {
        this.btEnvironmentOn = on;

    }

    public void notifyBTDiscovery(boolean on) {
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

    public void notifyOnlinePeersChanged(Set<CharSequence> newPeerList) {
        this.getASAPPeerApplicationSide().notifyOnlinePeersChanged(newPeerList);

        if(onlinePeerList.size() < newPeerList.size()) {
            Toast.makeText(this.getActivity(),
                    "new online connections", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this.getActivity(),
                    "online connections changed", Toast.LENGTH_SHORT).show();
        }

        this.onlinePeerList = newPeerList;

        // notify listeners - delegate
        this.getASAPPeerApplicationSide().notifyOnlinePeersChanged(newPeerList);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //                        asap received broadcast management                       //
    //                           is registered by ASAPActivity                         //
    /////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(this.getLogStart(), "received asap chunk received from asap engine/service");

        try {
            ASAPChunkReceivedBroadcastIntent asapReceivedIntent
                    = new ASAPChunkReceivedBroadcastIntent(intent);

            // delegate to local peer proxy
            this.getASAPPeerApplicationSide().chunkReceived(
                    asapReceivedIntent.getFormat().toString(),
                    asapReceivedIntent.getUser().toString(),
                    asapReceivedIntent.getUri().toString(),
                    asapReceivedIntent.getEra());
        } catch (ASAPException | IOException e) {
            Log.w(this.getLogStart(), "could call chunk received in local peer proxy: "
                    + e.getLocalizedMessage());
        }
    }

    /////// message received is triggered when chunks received
    @Override
    public void addASAPMessageReceivedListener(
            CharSequence format, ASAPMessageReceivedListener listener) {

        this.getASAPPeerApplicationSide().addASAPMessageReceivedListener(format, listener);
    }

    @Override
    public void removeASAPMessageReceivedListener(
            CharSequence format, ASAPMessageReceivedListener listener) {

        this.getASAPPeerApplicationSide().removeASAPMessageReceivedListener(format, listener);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                         other listeners beside chunk received                             //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /*
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
                try {
                    messageListener.asapMessagesReceived(asapMessages);
                } catch (IOException e) {
                    Log.e(this.getLogStart(), "calling messageReceivedListener: "
                            + e.getLocalizedMessage());
                }
            }
        }
    }
     */

    /////// uri changed listener - under construction - not yet support with ASAPPeer and not yet documented in web page

    private Map<CharSequence, Collection<ASAPUriContentChangedListener>> uriContentChangedListener
            = new HashMap<>();

    public final void addASAPUriContentChangedListener(
            CharSequence format, ASAPUriContentChangedListener listener) {
        Log.e(this.getLogStart(), "URI Content Changed currently under construction - will most probably fail");
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

        Log.e(this.getLogStart(), "URI Content Changed currently under construction - will most probably fail");
        Log.d(this.getLogStart(), "going to remove asap uri changed listener for " + format);

        if(uriChangedListeners != null) {
            uriChangedListeners.remove(listener);
        }
    }

    /////// online peers list and change notifications
    private Set<CharSequence> onlinePeerList = new HashSet<>();

    public Set<CharSequence> getOnlinePeerList() {
        return this.onlinePeerList;
    }

    @Override
    public void addASAPEnvironmentChangesListener(ASAPEnvironmentChangesListener listener) {
        this.getASAPPeerApplicationSide().addASAPEnvironmentChangesListener(listener);
    }

    @Override
    public void removeASAPEnvironmentChangesListener(ASAPEnvironmentChangesListener listener) {
        this.getASAPPeerApplicationSide().removeASAPEnvironmentChangesListener(listener);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                     component support                                     //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public void setupDrawerLayout(Activity activity) {
        Log.d(this.getLogStart(), "setupDrawerLayout dummy called: could be overwritten if needed. Don't do anything here");
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                        ASAPSimplePeer                                     //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CharSequence getPeerName() {
        return this.asapOwner;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                          ASAP messages are sent with the service                          //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void sendASAPMessage(CharSequence format, CharSequence uri, byte[] message)
            throws ASAPException {

        Activity activity = this.getActivity();
        if(activity == null) throw new ASAPException("no active activity");

        if(! (activity instanceof ASAPActivity))
            throw new ASAPException("current activity not derived from ASAPActivity");

        ASAPActivity asapActivity = (ASAPActivity)activity;
        asapActivity.sendASAPMessage(format, uri, message, true);
    }

    private String getLogStart() {
//        int objectID = this.hashCode();
//        return "ASAPApplication(" + objectID + ")";
        return Util.getLogStart(this);
    }
}
