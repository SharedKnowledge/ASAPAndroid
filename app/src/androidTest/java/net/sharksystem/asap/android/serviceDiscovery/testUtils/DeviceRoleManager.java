package net.sharksystem.asap.android.serviceDiscovery.testUtils;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Differentiates between 3 defined devices at runtime
 * which allows to execute different code on them.
 * <p>
 * This is used in the "live" test, where 3 devices
 * take on different roles to test the service discovery
 * <p>
 * The devices need to be defined {@link #getCurrentDeviceName()}
 * can be used to obtain the device names.
 * Also the Wifi and Bluetooth MAC addresses need to be specified
 * there is no way do get those in code (since Android 6)
 *
 * @author WilliBoelke
 */
public class DeviceRoleManager
{

    //
    //  ----------  the devices names ----------
    //

    public static final String DEVICE_A = "samsungSM-T580";
    public static final String DEVICE_B = "LENOVOLenovo TB-X304L";
    public static final String DEVICE_C = "DOOGEEY8";


    //
    //  ----------  the mac addresses of the devices ----------
    //

    public static final String MAC_A_BT = "D0:7F:A0:D6:1C:9A";
    public static final String MAC_B_BT = "D0:F8:8C:2F:19:9F";
    public static final String MAC_C_BT = "20:19:08:15:56:13";

    public static final String MAC_A_WIFI = "";
    public static final String MAC_B_WIFI = "d2:f8:8c:32:19:9f";
    public static final String MAC_C_WIFI = "02:27:15:ba:be:40";


    private static String runningDevice;

    /**
     * Returns a string containing the device name and manufacturer,
     * to distinct between devices at runtime
     *
     * @return a device name string
     */
    public static String getCurrentDeviceName()
    {
        //----------------------------------
        // NOTE : the actual mac address of
        // the local device would have been better
        // since then they could have been compared
        // more easily
        //----------------------------------
        return android.os.Build.MANUFACTURER + android.os.Build.MODEL;
    }

    /**
     * Determines which of the defined devices is running the tests
     */
    public static void determineTestRunner()
    {
        switch (getCurrentDeviceName())
        {
            case DEVICE_A:
                runningDevice = DEVICE_A;
                break;
            case DEVICE_B:
                runningDevice = DEVICE_B;
                break;
            case DEVICE_C:
                runningDevice = DEVICE_C;
                break;
            default:
                System.out.println("device not specified " + getCurrentDeviceName());
        }
    }

    /**
     * Returns the device running the tests
     *
     * @return
     */
    public static String getTestRunner()
    {
        return runningDevice;
    }

    public static void printDeviceInfo(Context context)
    {
        // https://developer.android.com/about/versions/marshmallow/android-6.0-changes#behavior-hardware-id
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Log.d("", "-------------------\n" +
                "device name = " + getCurrentDeviceName() + "\n" +
                "bt mac addr = " + BluetoothAdapter.getDefaultAdapter().getAddress() + "\n" +
                "wf mac addr = " + manager.getConnectionInfo().getMacAddress() + "\n" +
                "the mac addresses cant be obtained programmatically anymore (since android 6)" +
                "u need to find them manually");
    }
}
