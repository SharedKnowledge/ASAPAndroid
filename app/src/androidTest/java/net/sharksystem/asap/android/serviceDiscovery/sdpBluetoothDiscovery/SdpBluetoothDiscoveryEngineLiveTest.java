package net.sharksystem.asap.android.serviceDiscovery.sdpBluetoothDiscovery;

import static net.sharksystem.asap.android.serviceDiscovery.testUtils.DeviceRoleManager.DEVICE_A;
import static net.sharksystem.asap.android.serviceDiscovery.testUtils.DeviceRoleManager.DEVICE_B;
import static net.sharksystem.asap.android.serviceDiscovery.testUtils.DeviceRoleManager.DEVICE_C;
import static net.sharksystem.asap.android.serviceDiscovery.testUtils.DeviceRoleManager.MAC_B_BT;
import static net.sharksystem.asap.android.serviceDiscovery.testUtils.DeviceRoleManager.MAC_C_BT;
import static net.sharksystem.asap.android.serviceDiscovery.testUtils.DeviceRoleManager.determineTestRunner;
import static net.sharksystem.asap.android.serviceDiscovery.testUtils.DeviceRoleManager.getCurrentDeviceName;
import static net.sharksystem.asap.android.serviceDiscovery.testUtils.DeviceRoleManager.getTestRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.arch.core.executor.testing.CountingTaskExecutorRule;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import net.sharksystem.asap.android.serviceDiscovery.serviceDescription.ServiceDescription;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * The tests aim to test {@link SdpBluetoothDiscoveryEngine}
 * on actual hardware.
 * <p>---------------------------------------------<p>
 * This is more experimental, and i aim to improve on
 * that in the future, but i could find another good way
 * to make this kind of tests, though...i cant be the only one needing this.
 * <p>
 * The tests can fail, they are performed on actual hardware
 * it comes to timing issues between the devices.
 * Sometimes a discovery just does not find the service or device in
 * the specified amount of time.
 * <p>
 * If all test fail check the following :
 * * device names specified ? <br>
 * * wifi / bluetooth available and on ?<br>
 * * in case of bluetooth i observed that UUIDs arent reliably
 * exchanged when a device is low on battery<br>
 * * in case of bluetooth make sure to press the alter dialogue
 * to allow discoverability (sorry cant change that apparently)<br>
 * * check if the tests get out of sync, maybe one adb connection is
 * much slower then the others ?<br>
 * <p>
 * <p>
 * The tests need to run in sync on 3
 * different devices, depending on the adb connection to each
 * and the speed of the devices themself it can fall out of sync.
 * I will try to find a better solution in the future.
 * <p>
 * Also regarding the timing issues- it helps to not run all the tests
 * sequentially, because then delays add up.- maybe run tests one at a time
 * i know that's not really automated (but since the alter dialogue pops up always
 * there need to be someone managing it either way).
 * For that also keep an eye on this:
 * https://stackoverflow.com/questions/73418555/disable-system-alertdialog-in-android-instrumented-tests
 * maybe there will be an answer.
 * <p>---------------------------------------------<p>
 * These tests are to be performed on 3
 * physical devices.
 * The devices are required to have Wifi Direct  set to on.
 * The devices all need to run the tests simultaneously.
 * <p>
 * To run the test a few configurations are needed, to differentiate
 * their names need to be specified beforehand. Same goes for
 * their bluetooth and wifi mac addresses.
 * For that refer to the {@link net.sharksystem.asap.android.serviceDiscovery.testUtils.DeviceRoleManager}
 * ad specify them there.
 * <p>---------------------------------------------<p>
 * General premise - each test will be split into 3 different roles
 * which will execute a different code. Those are defined in additional methods
 * right below the test method itself, following the naming pattern
 * "testCaseName_roleSpecificName"
 *
 * @author WilliBoelke
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SdpBluetoothDiscoveryEngineLiveTest
{

    /**
     * Classname for logging
     */
    private final String TAG = this.getClass().getSimpleName();

    private ServiceDescription descriptionForServiceOne;
    private ServiceDescription descriptionForServiceTwo;
    private BluetoothAdapter adapter;

    @Rule
    public GrantPermissionRule fineLocationPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);
    @Rule
    public CountingTaskExecutorRule executionerRule = new CountingTaskExecutorRule();

    @BeforeClass
    public static void classSetup()
    {
        determineTestRunner();
    }

    @Before
    public void setup()
    {
        HashMap<String, String> serviceAttributesOne = new HashMap<>();
        HashMap<String, String> serviceAttributesTwo = new HashMap<>();
        serviceAttributesOne.put("service-name", "Test Service One");
        serviceAttributesOne.put("service-info", "This is a test service description");
        serviceAttributesTwo.put("service-name", "Counting Service Two");
        serviceAttributesTwo.put("service-info", "This is another test service description");
        descriptionForServiceOne = new ServiceDescription("test service one", serviceAttributesOne);
        descriptionForServiceTwo = new ServiceDescription("test service two", serviceAttributesTwo);
        SdpBluetoothDiscoveryEngine.getInstance().start(InstrumentationRegistry.getInstrumentation().getTargetContext());
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    @After
    public void teardown() throws NullPointerException
    {
        SdpBluetoothDiscoveryEngine.getInstance().teardownEngine();
    }


    //
    //  ----------  discovers nearby devices ----------
    //

    @Test
    public void itShouldFindNearbyDevice() throws InterruptedException
    {
        switch (getTestRunner())
        {
            case DEVICE_A:
                itShouldFindNearbyDevice_discovery();
                break;
            case DEVICE_B:
                itShouldFindNearbyDevice_discoverable();
                break;
            case DEVICE_C:
                itShouldFindNearbyDevice_discoverable();
            default:
                Log.e(TAG, "device not specified " + getCurrentDeviceName());
        }
    }

    /**
     * Starts the device discoverability
     */
    private void itShouldFindNearbyDevice_discoverable() throws InterruptedException
    {
        startDiscoverable();
        assertTrue(true); // test shouldn't fail on this device
        synchronized (this)
        {
            this.wait(13000); // device discovery takes about 12s
        }
    }

    /**
     * Starts the device discovery and checks if the other two devices where found by
     * looking for their mac addresses
     */
    private void itShouldFindNearbyDevice_discovery() throws InterruptedException
    {


        ArrayList<BluetoothDevice> discoveredDevices = new ArrayList<>();

        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(new BluetoothServiceDiscoveryListener()
        {

            @Override
            public void onServiceDiscovered(BluetoothDevice host, ServiceDescription description)
            {

            }

            @Override
            public void onPeerDiscovered(BluetoothDevice device)
            {
                discoveredDevices.add(device);
            }
        });
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery();
        synchronized (this)
        {
            this.wait(13000); // device discovery takes about 12s
        }
        assertTrue(discoveredDevices.contains(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_B_BT)));
        assertTrue(discoveredDevices.contains(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_C_BT)));
    }


    //
    //  ----------  discovers one nearby service ----------
    //

    @Test
    public void itShouldFindOneNearbyAvailableService() throws InterruptedException
    {
        switch (getTestRunner())
        {
            case DEVICE_A:
                itShouldFindOneNearbyAvailableServices_serviceDiscovery();
                break;
            case DEVICE_B:
                itShouldFindOneNearbyAvailableService_serviceAdvertisement();
                break;
            case DEVICE_C:
                synchronized (this)
                {
                    this.wait(31000); // wait for test to finish
                }
                assertTrue(true); // test shouldn't fail on this device
            default:
                Log.e(TAG, "device not specified " + getCurrentDeviceName());
        }
    }


    /**
     * Starts the device discoverability and a service {@link #descriptionForServiceOne}
     */
    private void itShouldFindOneNearbyAvailableService_serviceAdvertisement() throws InterruptedException
    {
        ServiceAdvertisementThread thread = new ServiceAdvertisementThread(adapter, descriptionForServiceOne);
        thread.startService();
        this.startDiscoverable();
        synchronized (this)
        {
            this.wait(30000); // wait for test to finish
        }
        thread.cancel();
        assertTrue(true); // test shouldn't fail on this device
    }

    /**
     * Starts the service discovery and checks if the service was found
     */
    private void itShouldFindOneNearbyAvailableServices_serviceDiscovery() throws InterruptedException
    {
        ArrayList<BluetoothDevice> serviceHosts = new ArrayList<>();
        ArrayList<ServiceDescription> services = new ArrayList<>();

        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(new BluetoothServiceDiscoveryListener()
        {
            @Override
            public void onServiceDiscovered(BluetoothDevice host, ServiceDescription description)
            {
                serviceHosts.add(host);
                services.add(description);
            }

            @Override
            public void onPeerDiscovered(BluetoothDevice device)
            {
                // not to test here
            }
        });

        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(descriptionForServiceOne);
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery();

        synchronized (this)
        {
            this.wait(30000); // this is the maximum time i give it to find the service
        }

        assertTrue(serviceHosts.contains(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_B_BT)));
        assertTrue(services.contains(descriptionForServiceOne));
    }


    //
    //  ----------  discovers 2 services on the same device  ----------
    //


    @Test
    public void itShouldFindTwoNearbyAvailableService() throws InterruptedException
    {
        switch (getTestRunner())
        {
            case DEVICE_A:
                itShouldFindTwoNearbyAvailableService_serviceDiscovery();
                break;
            case DEVICE_B:
            case DEVICE_C:
                itShouldFindTwoNearbyAvailableService_serviceAdvertisement();
                break;
            default:
                Log.e(TAG, "device not specified " + getCurrentDeviceName());
        }
    }

    /**
     * Starts the device discoverability and a service {@link #descriptionForServiceOne}
     */
    private void itShouldFindTwoNearbyAvailableService_serviceAdvertisement() throws InterruptedException
    {
        ServiceAdvertisementThread thread = new ServiceAdvertisementThread(adapter, descriptionForServiceOne);
        thread.startService();
        this.startDiscoverable();
        synchronized (this)
        {
            this.wait(30000); // wait for test to finish
        }
        thread.cancel();
        assertTrue(true); // test shouldn't fail on this device
    }

    /**
     * Starts the service discovery and checks if the service was found
     */
    private void itShouldFindTwoNearbyAvailableService_serviceDiscovery() throws InterruptedException
    {
        ArrayList<BluetoothDevice> serviceHosts = new ArrayList<>();
        ArrayList<ServiceDescription> services = new ArrayList<>();

        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(new BluetoothServiceDiscoveryListener()
        {
            @Override
            public void onServiceDiscovered(BluetoothDevice host, ServiceDescription description)
            {
                serviceHosts.add(host);
                services.add(description);
            }

            @Override
            public void onPeerDiscovered(BluetoothDevice device)
            {
                // not to test here
            }
        });

        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(descriptionForServiceOne);
        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(descriptionForServiceTwo);
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery();

        synchronized (this)
        {
            this.wait(30000); // this is the maximum time i give it to find the service
        }

        assertTrue(serviceHosts.contains(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_B_BT)));
        assertTrue(serviceHosts.contains(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_C_BT)));
        assertTrue(services.contains(descriptionForServiceOne));
    }


    //
    //  ----------  it can find two different services on a single device ----------
    //


    @Test
    public void itShouldFindTwoDifferentServicesOnOneDevice() throws InterruptedException
    {
        switch (getTestRunner())
        {
            case DEVICE_A:
                itShouldFindTwoDifferentServices_serviceDiscovery();
                break;
            case DEVICE_B:
                synchronized (this)
                {
                    this.wait(31000); // wait for test to finish
                }
                break;
            case DEVICE_C:
                itShouldFindTwoDifferentServices_serviceAdvertisement();
                break;
            default:
                Log.e(TAG, "device not specified " + getCurrentDeviceName());
        }
    }

    /**
     * Starts the device discoverability and a service {@link #descriptionForServiceOne}
     */
    private void itShouldFindTwoDifferentServices_serviceAdvertisement() throws InterruptedException
    {
        ServiceAdvertisementThread threadOne = new ServiceAdvertisementThread(adapter, descriptionForServiceOne);
        threadOne.startService();
        ServiceAdvertisementThread threadTwo = new ServiceAdvertisementThread(adapter, descriptionForServiceTwo);
        threadTwo.startService();
        this.startDiscoverable();
        synchronized (this)
        {
            this.wait(31000); // wait for test to finish
        }
        threadOne.cancel();
        threadTwo.cancel();
        assertTrue(true); // test shouldn't fail on this device
    }

    /**
     * Starts the service discovery and checks if the service was found
     */
    private void itShouldFindTwoDifferentServices_serviceDiscovery() throws InterruptedException
    {
        ArrayList<BluetoothDevice> serviceHosts = new ArrayList<>();
        ArrayList<ServiceDescription> services = new ArrayList<>();

        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(new BluetoothServiceDiscoveryListener()
        {
            @Override
            public void onServiceDiscovered(BluetoothDevice host, ServiceDescription description)
            {
                serviceHosts.add(host);
                services.add(description);
            }

            @Override
            public void onPeerDiscovered(BluetoothDevice device)
            {
                // not to test here
            }
        });

        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(descriptionForServiceOne);
        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(descriptionForServiceTwo);
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery();

        synchronized (this)
        {
            this.wait(30000); // this is the maximum time i give it to find the service
        }

        assertTrue(serviceHosts.contains(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_C_BT)));
        assertTrue(services.contains(descriptionForServiceOne));
        assertTrue(services.contains(descriptionForServiceTwo));
    }


    //
    //  ----------  it finds two different services on separate devices ----------
    //


    @Test
    public void itShouldFindTwoDifferentServicesOnSeparateDevice() throws InterruptedException
    {
        switch (getTestRunner())
        {
            case DEVICE_A:
                itShouldFindTwoDifferentServicesOnSeparateDevice_serviceDiscovery();
                break;
            case DEVICE_B:
                itShouldFindTwoDifferentServicesOnSeparateDevice_serviceAdvertisement_A();
                break;
            case DEVICE_C:
                itShouldFindTwoDifferentServicesOnSeparateDevice_serviceAdvertisement_B();
                break;
            default:
                Log.e(TAG, "device not specified " + getCurrentDeviceName());
        }
    }


    private void itShouldFindTwoDifferentServicesOnSeparateDevice_serviceAdvertisement_B() throws InterruptedException
    {
        ServiceAdvertisementThread thread = new ServiceAdvertisementThread(adapter, descriptionForServiceOne);
        thread.startService();
        this.startDiscoverable();
        synchronized (this)
        {
            this.wait(30000); // wait for test to finish
        }
        thread.cancel();
        assertTrue(true); // test shouldn't fail on this device
    }

    private void itShouldFindTwoDifferentServicesOnSeparateDevice_serviceAdvertisement_A() throws InterruptedException
    {
        ServiceAdvertisementThread thread = new ServiceAdvertisementThread(adapter, descriptionForServiceOne);
        thread.startService();
        this.startDiscoverable();
        synchronized (this)
        {
            this.wait(30000); // wait for test to finish
        }
        thread.cancel();
        assertTrue(true); // test shouldn't fail on this device
    }


    private void itShouldFindTwoDifferentServicesOnSeparateDevice_serviceDiscovery() throws InterruptedException
    {
        ArrayList<BluetoothDevice> serviceHosts = new ArrayList<>();
        ArrayList<ServiceDescription> services = new ArrayList<>();

        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(new BluetoothServiceDiscoveryListener()
        {
            @Override
            public void onServiceDiscovered(BluetoothDevice host, ServiceDescription description)
            {
                serviceHosts.add(host);
                services.add(description);
            }

            @Override
            public void onPeerDiscovered(BluetoothDevice device)
            {
                // not to test here
            }
        });

        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(descriptionForServiceOne);
        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(descriptionForServiceTwo);
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery();

        synchronized (this)
        {
            this.wait(30000); // this is the maximum time i give it to find the service
        }

        assertTrue(serviceHosts.contains(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_C_BT)));
        assertTrue(serviceHosts.contains(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_B_BT)));
        assertTrue(services.contains(descriptionForServiceOne));
        assertTrue(services.contains(descriptionForServiceTwo));
    }


    @Test
    public void itShouldNotifiedAboutMatchingServicesAlreadyDiscovered() throws InterruptedException
    {
        switch (getTestRunner())
        {
            case DEVICE_A:
                itShouldNotifiedAboutMatchingServicesAlreadyDiscovered_serviceDiscovery();
                break;
            case DEVICE_B:
            case DEVICE_C:
                itShouldNotifiedAboutMatchingServicesAlreadyDiscovered_serviceAdvertisement();
                break;
            default:
                Log.e(TAG, "device not specified " + getCurrentDeviceName());
        }
    }

    private void itShouldNotifiedAboutMatchingServicesAlreadyDiscovered_serviceAdvertisement() throws InterruptedException
    {
        ServiceAdvertisementThread thread = new ServiceAdvertisementThread(adapter, descriptionForServiceOne);
        thread.startService();
        this.startDiscoverable();
        synchronized (this)
        {
            this.wait(30000); // wait for test to finish
        }
        thread.cancel();
        assertTrue(true); // test shouldn't fail on this device
    }

    private void itShouldNotifiedAboutMatchingServicesAlreadyDiscovered_serviceDiscovery() throws InterruptedException
    {
        ArrayList<BluetoothDevice> serviceHosts = new ArrayList<>();
        ArrayList<ServiceDescription> services = new ArrayList<>();

        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(new BluetoothServiceDiscoveryListener()
        {
            @Override
            public void onServiceDiscovered(BluetoothDevice host, ServiceDescription description)
            {
                serviceHosts.add(host);
                services.add(description);
            }

            @Override
            public void onPeerDiscovered(BluetoothDevice device)
            {
                // not to test here
            }
        });

        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery();

        synchronized (this)
        {
            this.wait(25000); // this is the maximum time i give it to find the service
        }

        assertEquals(0, serviceHosts.size());

        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(descriptionForServiceOne);
        synchronized (this)
        {
            this.wait(5000); // this is the maximum time i give it to find the service
        }
        assertTrue(serviceHosts.contains(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_C_BT)));
        assertTrue(serviceHosts.contains(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_B_BT)));
        assertTrue(services.contains(descriptionForServiceOne));
    }


    /**
     * This thread is just for starting a bluetooth Server socket
     * and advertise a service through that
     */
    private class ServiceAdvertisementThread extends Thread
    {

        /**
         * Log Tag
         */
        private final String TAG = this.getClass().getSimpleName();

        /**
         * The BluetoothAdapter
         */
        private final BluetoothAdapter mBluetoothAdapter;

        private final ServiceDescription description;

        /**
         * Bluetooth server socket to accept incoming connections
         */
        private BluetoothServerSocket serverSocket;

        private boolean running;

        private Thread thread;


        //
        //  ----------  constructor and initialisation ----------
        //

        /**
         * Constructor
         *
         * @param bluetoothAdapter
         *         The BluetoothAdapter tto use (usually the defaultAdapter)
         */
        public ServiceAdvertisementThread(BluetoothAdapter bluetoothAdapter, ServiceDescription description)
        {
            this.mBluetoothAdapter = bluetoothAdapter;
            this.description = description;
            this.running = true;
        }

        //
        //  ----------  start  ----------
        //

        public synchronized void startService()
        {
            Log.d(TAG, "startService : starting Bluetooth Service");
            openServerSocket();
            this.start();
        }

        private void openServerSocket()
        {
            Log.d(TAG, "openServerSocket: opening server socket with UUID : " + description.getServiceUuid());
            try
            {
                this.serverSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(description.getServiceUuid().toString(), description.getServiceUuid());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        //
        //  ----------  run ----------
        //

        public void run()
        {
            this.thread = currentThread();
            while (this.running)
            {
                acceptConnections();
            }
            Log.d(TAG, "run: Accept thread ended final");
        }

        /**
         * Advertises the service with the uuid given through {@link #description}
         */
        private void acceptConnections()
        {
            Log.d(TAG, "run:  Thread started");
            BluetoothSocket socket = null;
            //Blocking Call : Accept thread waits here till another device connects (or canceled)
            Log.d(TAG, "run: RFCOMM server socket started, waiting for connections ...");
            try
            {
                socket = this.serverSocket.accept();
                Log.d(TAG, "run: RFCOMM server socked accepted client connection");
            }
            catch (IOException e)
            {
                Log.e(TAG, "acceptConnections: an IOException occurred, trying to fix");
                try
                {
                    Log.e(TAG, "acceptConnections: trying to close socket");
                    this.serverSocket.close();
                }
                catch (IOException e1)
                {
                    Log.e(TAG, "acceptConnections: could not close the socket");
                }
                Log.e(TAG, "acceptConnections: trying to open new server socket");
                this.openServerSocket();
            }
            if (socket == null)
            {
                Log.d(TAG, "run: Thread was interrupted");
                return;
            }
            Log.d(TAG, "run:  service accepted client connection, opening streams");
        }

        //
        //  ----------  end ----------
        //

        public void cancel()
        {
            Log.d(TAG, "cancel: cancelling accept thread");
            this.running = false;
            if (this.thread != null)
            {
                this.thread.interrupt();
                Log.d(TAG, "cancel: accept thread interrupted");
            }
            try
            {
                this.serverSocket.close();
                Log.d(TAG, "cancel: closed AcceptThread");
            }
            catch (NullPointerException | IOException e)
            {
                Log.e(TAG, "cancel: socket was null", e);
            }
        }

        //
        //  ----------  getter and setter ----------
        //
    }

    /**
     * Asks user (the tester in this case) to make device discoverable
     */
    public void startDiscoverable()
    {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
        discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // tests  wand we do add this
        InstrumentationRegistry.getInstrumentation().getTargetContext().startActivity(discoverableIntent);
    }
}