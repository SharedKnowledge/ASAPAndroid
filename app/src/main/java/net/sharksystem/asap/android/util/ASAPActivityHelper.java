package net.sharksystem.asap.android.util;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import net.sharksystem.asap.android.ASAP;
import net.sharksystem.asap.android.ASAPService;
import net.sharksystem.asap.android.ASAPServiceMethods;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.support.v4.content.PermissionChecker.PERMISSION_DENIED;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class ASAPActivityHelper
        implements ASAPServiceRequestListener, ASAPServiceNotificationListener {

    private static final int MY_REQUEST_2ENABLE_BT = 1;
    private static final int MY_ASK_FOR_PERMISSIONS_REQUEST = 100;

    private final Activity activity;
    private final CharSequence asapOwner;
    private final ASAPServiceNotificationListener notificationListener;
    private Messenger mService;
    private boolean mBound;

    private List<String> requiredPermissions;
    private List<String> grantedPermissions = new ArrayList<>();
    private List<String> deniedPermissions = new ArrayList<>();

    public ASAPActivityHelper(Activity activity,
            ASAPServiceNotificationListener notificationListener, CharSequence asapOwner) {

        this.activity = activity;
        this.notificationListener = notificationListener;
        this.asapOwner = asapOwner;

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
        Intent asapServiceCreationIntent = new Intent(activity, ASAPService.class);
        asapServiceCreationIntent.putExtra(ASAP.USER, asapOwner);

        activity.startService(asapServiceCreationIntent);

        ASAPServiceRequestNotifyBroadcastReceiver srbc =
                new ASAPServiceRequestNotifyBroadcastReceiver(this, this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ASAPServiceRequestNotifyIntent.ASAP_SERVICE_REQUEST_ACTION);
        activity.registerReceiver(srbc, filter);
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

    @Override
    public void asapSrcRq_enableBluetooth() {
        // check if bt is enabled
        // get default bt adapter - there could be proprietary adapters
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter == null) {
            // Device doesn't support Bluetooth
            Log.i(this.getLogStart(), "device does not support bluetooth - give up");
            Toast t = Toast.makeText(this.activity,
                    "Device does not support Bluetooth - give up", Toast.LENGTH_LONG);
            t.show();
            return;
        }

        Log.d(this.getLogStart(), "device has a bluetooth adapter");

        // those things are to be done in calling activity
        if (!defaultAdapter.isEnabled()) {
            Log.d(this.getLogStart(), "BT disabled - ask user to enable");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.activity.startActivityForResult(enableBtIntent, MY_REQUEST_2ENABLE_BT);
            // issued - wait for reply
            return;
        }
    }

    private int visibilityTime = 1;

    @Override
    public void asapSrcRq_startBTDiscoverable(int time) {
        this.visibilityTime = time;
        Log.d(this.getLogStart(), "going to make device bt visibile for seconds: "
                + this.visibilityTime);

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                this.visibilityTime);

        // ask user to confirm - result is passed to onActivityResult
        this.activity.startActivity(discoverableIntent);
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(this.getLogStart(),
                "requestCode == " + requestCode +
                        " / resultCode == " + resultCode);

        if(requestCode == MY_REQUEST_2ENABLE_BT && resultCode == RESULT_OK) {
            Log.d(this.getLogStart(), "Bluetooth now enabled - ask service to start BT");
            this.sendMessage2Service(ASAPServiceMethods.START_BLUETOOTH);
        }

        if(resultCode == this.visibilityTime) {
            Log.d(this.getLogStart(), "user allowed BT discoverability for seconds: "
                    + this.visibilityTime);
        }
    }

    public void sendMessage2Service(int messageNumber) {
        if(this.mService == null) {
            Log.d(this.getLogStart(), "service not yet available - cannot send message");
            return;
        }

        Message msg = Message.obtain(null, messageNumber, 0, 0);
        this.sendMessage2Service(msg);
    }

    public void sendMessage2Service(Message msg) {
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //                                       life cycle                                //
    /////////////////////////////////////////////////////////////////////////////////////

    public void onStart() {
        // Bind to the service
        this.activity.bindService(new Intent(this.activity, ASAPService.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    public void onStop() {
        // Unbind from the service
        if (mBound) {
            this.activity.unbindService(mConnection);
            mBound = false;
        }
        this.sendMessage2Service(ASAPServiceMethods.STOP_WIFI_DIRECT);
    }

    public void onDestroy() {
        // stop
        this.sendMessage2Service(ASAPServiceMethods.STOP_WIFI_DIRECT);
        this.sendMessage2Service(ASAPServiceMethods.STOP_BLUETOOTH);
        this.sendMessage2Service(ASAPServiceMethods.STOP_BROADCASTS);

        // and kill service itself
        this.activity.stopService(new Intent(this.activity, ASAPService.class));
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };

    @Override
    public void asapNotifyBTDiscoverableStopped() {
        this.notificationListener.asapNotifyBTDiscoverableStopped();
    }

    @Override
    public void aspNotifyBTDiscoveryStopped() {
        this.notificationListener.aspNotifyBTDiscoveryStopped();
    }

    @Override
    public void aspNotifyBTDiscoveryStarted() {
        this.notificationListener.aspNotifyBTDiscoveryStarted();
    }

    @Override
    public void aspNotifyBTDiscoverableStarted() {
        this.notificationListener.aspNotifyBTDiscoverableStarted();
    }
}
