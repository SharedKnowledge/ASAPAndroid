package net.sharksystem.asap.android;

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

import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceNotificationListener;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestListener;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyBroadcastReceiver;
import net.sharksystem.asap.android.service2AppMessaging.ASAPServiceRequestNotifyIntent;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.support.v4.content.PermissionChecker.PERMISSION_DENIED;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class ASAPApplication {
    private static final int MY_ASK_FOR_PERMISSIONS_REQUEST = 100;
    private final CharSequence asapOwner;
    private boolean initialized = false;

    private boolean btDisoverableOn = false;
    private boolean btDisoveryOn = false;
    private boolean btEnvironmentOn = false;

    private Activity activity;

    private List<String> requiredPermissions;
    private List<String> grantedPermissions = new ArrayList<>();
    private List<String> deniedPermissions = new ArrayList<>();
    private int activityCount = 0;

    protected ASAPApplication(CharSequence asapOwner) {
        this.asapOwner = asapOwner;
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
            Intent asapServiceCreationIntent = new Intent(activity, ASAPService.class);
            asapServiceCreationIntent.putExtra(ASAP.USER, this.asapOwner);

            this.activity.startService(asapServiceCreationIntent);

            this.initialized = true;
        }
    }

    public static ASAPApplication getASAPApplication() {
        return new ASAPApplication("DummyUser");
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

    protected Activity getActivity() {
        return this.activity;
    }

    void setActivity(Activity activity) {
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

    /*
    public void stopAll() {
        this.sendMessage2Service(ASAPServiceMethods.STOP_WIFI_DIRECT);
        this.sendMessage2Service(ASAPServiceMethods.STOP_BLUETOOTH);
        this.sendMessage2Service(ASAPServiceMethods.STOP_BROADCASTS);

        // and kill service itself
        this.activity.stopService(new Intent(this.activity, ASAPService.class));
    }

     */
}
