package net.sharksystem.asap.android.serviceDiscovery.sdpBluetoothDiscovery

import android.Manifest
import android.arch.core.executor.testing.CountingTaskExecutorRule
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.GrantPermissionRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import net.sharksystem.asap.android.serviceDiscovery.serviceDescription.ServiceDescription
import net.sharksystem.asap.android.serviceDiscovery.testUtils.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 *
 *  These are integration tests for the SdpBluetoothEngine and
 *  other subsequently used systems like the
 *  ServiceDescription and the SdpBluetoothDiscoveryEngine.
 *  <p>---------------------------------------------<p>
 *  For the tests the android bluetooth api is mocked.
 *  This makes it possible for the tests to run faster and
 *  test some more cases then letting them run on the actual api.
 *  <p>
 *  For Mocking the android api the mocking framework `Mockk`
 *  turned out to be a very good alternative to mockito, which
 *  has problems with mocking final and static classes and methods
 *  which is required to mock the android bt api and the context.
 *  Mockk is a Kotlin library thus Kotlin will be used for the test.
 *
 *  Also Mockk REQUIRES AT LEAST ANDROID P for some features,
 *  so to run the tests an emulator or device with android p is recommended.
 *  else some tests may fail
 *  <p>---------------------------------------------<p>
 *  Sine the complete bluetooth api is mocked, and the
 *  BroadcastReceivers are not actually functioning, test could be run
 *  ignoring the actual flow of events.
 *  for example `onUuidsFetched()`, could be called without
 *  prior `onDeviceDiscoveryFinished()`, this also would work.
 *  This would not happen normally, sine the fetching press
 *  will only be started after a device was discovered
 *  For the tests i wont do that, but emulate the Bluetooth API
 *  and behavior as close to the actual thing as possible.
 *  <p>---------------------------------------------<p>
 *  To test the engine we need to monitor its output and emulate its
 *  input:
 *  <p>
 *  -Output:
 *  The results of the SdpBluetoothEngines work can be monitored
 *  through implementing the client and server interfaces.
 *  In some cases method calls on mock objets need to be verified to
 *  see results. Especially on the BluetoothAdapter, which can be
 *  mocked and injected, method calls are a essential part of testing
 *  the engines work.
 *  <p>
 *  -Input:
 *  The SdpBluetoothEngines input comes from BroadcastReceivers or
 *  from the "user". The user input an be easily emulated using the
 *  public interface.
 *  <p>
 *  -Verify API usage:
 *  Since the api is mocked correct and expected method calls can be verified
 *  <p>---------------------------------------------<p>
 *  The BroadcastReceivers are separated form the engine and use
 *  protected methods (`onDeviceDiscovered` , `onUuidsFetched` and `onDeviceDiscoveryFinished`)
 *  to notify the engine and provide necrosis inputs.
 *  As stated those methods are protected and not part of the public interface.
 *  Java allows access to protected methods to other classes in the same directory
 *  (which includes tests, as long as they use the same package structure).
 *  Kotlin does not allow that.
 *  Since those methods should not be public at all, and they are still required for
 *  testing, reflections will be used to access them. {@see Utils.kt#callPrivateFunc}
 *
 * @author WilliBoelke
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class SdpBluetoothDiscoveryEngineMockTest {


    /**
     * Executing sequentially
     */
    @get:Rule
    val instantTaskExecutorRule = CountingTaskExecutorRule()

    @get:Rule
    var fineLocationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    lateinit var mockedBtAdapter: BluetoothAdapter
    lateinit var mockedContext: Context

    @Before
    fun setup() {
        //Setup
        mockedContext = mockk(relaxed = true)
        mockedBtAdapter = mockk()

        every { mockedBtAdapter.isEnabled } returns true
        every { mockedBtAdapter.isDiscovering } returns false
        every { mockedBtAdapter.startDiscovery() } returns true
        every { mockedBtAdapter.cancelDiscovery() } returns true
        every { mockedBtAdapter.startDiscovery() } returns true

        //Run
        SdpBluetoothDiscoveryEngine.getInstance().start(mockedContext, mockedBtAdapter)
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery()
        initTestMocks()
    }

    @After
    fun teardown() {
        SdpBluetoothDiscoveryEngine.getInstance().callPrivateFunc("teardownEngine")
    }

    @Test
    fun itShouldInitialize() {
        //Check
        assertTrue(SdpBluetoothDiscoveryEngine.getInstance() != null)
    }


    /**
     * It Should notify listeners about discovered peers (BluetoothDevice)
     * as soon as they are found
     */
    @Test
    fun itNotifiesAboutEveryDiscoveredPeer() {

        val foundDevices: ArrayList<BluetoothDevice> = ArrayList()
        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(object :
            BluetoothServiceDiscoveryListener {
            override fun onServiceDiscovered(
                host: BluetoothDevice?,
                description: ServiceDescription?
            ) {
                // not under test
            }

            override fun onPeerDiscovered(device: BluetoothDevice) {
                foundDevices.add(device)

            }
        })

        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery()
        Thread.sleep(500)
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", getTestDeviceOne())
        Thread.sleep(200)
        assertTrue(foundDevices.size == 1)
        Thread.sleep(200)
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", getTestDeviceTwo())
        Thread.sleep(200)
        assertTrue(foundDevices.size == 2)
    }


    /**
     *
     */
    @Test
    fun itFetchesUuidsOfAllDiscoveredDevicesAfterDeviceDiscoveryFinished() {
        val testDeviceOne = getTestDeviceOne()
        val testDeviceTwo = getTestDeviceTwo()
        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(object :
            BluetoothServiceDiscoveryListener {
            override fun onServiceDiscovered(
                host: BluetoothDevice?,
                description: ServiceDescription?
            ) {
                // not under test
            }

            override fun onPeerDiscovered(device: BluetoothDevice) {
                // not under test here
            }

        })
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery()
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceOne)
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceTwo)
        verify(exactly = 0) { testDeviceOne.fetchUuidsWithSdp() }
        verify(exactly = 0) { testDeviceTwo.fetchUuidsWithSdp() }
        verify(exactly = 0) { testDeviceOne.fetchUuidsWithSdp() }
        verify(exactly = 0) { testDeviceTwo.fetchUuidsWithSdp() }

        // discovery finished:
        SdpBluetoothDiscoveryEngine.getInstance().callPrivateFunc("onDeviceDiscoveryFinished")
        verify(exactly = 1) { testDeviceOne.fetchUuidsWithSdp() }
        verify(exactly = 1) { testDeviceTwo.fetchUuidsWithSdp() }
    }


    @Test
    fun itShouldFetchUuidsWhenRefreshStarted() {
        val testDeviceOne = getTestDeviceOne()
        val testDeviceTwo = getTestDeviceTwo()

        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(object :
            BluetoothServiceDiscoveryListener {
            override fun onServiceDiscovered(
                host: BluetoothDevice?,
                description: ServiceDescription?
            ) {
                // not under test
            }

            override fun onPeerDiscovered(device: BluetoothDevice) {
                // not under test here
            }

        })

        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery()
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceOne)
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceTwo)
        verify(exactly = 0) { testDeviceOne.fetchUuidsWithSdp() }
        verify(exactly = 0) { testDeviceTwo.fetchUuidsWithSdp() }
        verify(exactly = 0) { testDeviceOne.fetchUuidsWithSdp() }
        verify(exactly = 0) { testDeviceTwo.fetchUuidsWithSdp() }
        // discovery finished:
        SdpBluetoothDiscoveryEngine.getInstance().callPrivateFunc("onDeviceDiscoveryFinished")
        verify(exactly = 1) { testDeviceOne.fetchUuidsWithSdp() }
        verify(exactly = 1) { testDeviceTwo.fetchUuidsWithSdp() }

        SdpBluetoothDiscoveryEngine.getInstance().refreshNearbyServices()
        verify(exactly = 2) { testDeviceOne.fetchUuidsWithSdp() }
        verify(exactly = 2) { testDeviceTwo.fetchUuidsWithSdp() }
    }


    @Test
    fun itFindsServices() {
        val testDeviceOne = getTestDeviceOne()
        val testDeviceTwo = getTestDeviceTwo()
        val foundDevices: ArrayList<BluetoothDevice> = ArrayList()
        val foundServices: ArrayList<ServiceDescription> = ArrayList()
        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(object :
            BluetoothServiceDiscoveryListener {
            override fun onServiceDiscovered(
                host: BluetoothDevice,
                description: ServiceDescription
            ) {
                foundDevices.add(host)
                foundServices.add(description)
            }

            override fun onPeerDiscovered(device: BluetoothDevice) {
                // not under test here
            }

        })

        // testDescription one has the testUuidTwo
        // which is part of testDeviceOne
        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(testDescriptionTwo)
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery()
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceOne)
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceTwo)

        assertTrue(foundDevices.isEmpty())
        assertTrue(foundServices.isEmpty())
        // discovery finished:
        SdpBluetoothDiscoveryEngine.getInstance().callPrivateFunc("onDeviceDiscoveryFinished")

        verify(exactly = 1) { testDeviceOne.fetchUuidsWithSdp() }
        verify(exactly = 1) { testDeviceTwo.fetchUuidsWithSdp() }

        // finds the service
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onUuidsFetched", testDeviceOne, getTestUuidArrayOne())
        assertTrue(foundDevices[0] == testDeviceOne)
        assertTrue(foundServices[0] == testDescriptionTwo)

        // finds services it does not look for
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onUuidsFetched", testDeviceTwo, getTestUuidArrayTwo())
        assertTrue(foundDevices.size == 1)
        assertTrue(foundServices.size == 1)
        assertTrue(foundDevices[0] == testDeviceOne)
        assertTrue(foundServices[0] == testDescriptionTwo)
    }

    /**
     * The Engine can search for several services at a time
     */
    @Test
    fun itShouldBeAbleToSearchForSeveralServicesAtATime() {


        val foundDevices: ArrayList<BluetoothDevice> = ArrayList()
        val foundServices: ArrayList<ServiceDescription> = ArrayList()
        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(object :
            BluetoothServiceDiscoveryListener {
            override fun onServiceDiscovered(
                host: BluetoothDevice,
                description: ServiceDescription
            ) {
                foundDevices.add(host)
                foundServices.add(description)
            }

            override fun onPeerDiscovered(device: BluetoothDevice) {
                // not under test here
            }

        })
        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(testDescriptionFive)
        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(testDescriptionTwo)
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery()

        // discovered device
        val testDeviceOne = getTestDeviceOne()
        val testDeviceTwo = getTestDeviceTwo()
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceOne)
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceTwo)
        SdpBluetoothDiscoveryEngine.getInstance().callPrivateFunc("onDeviceDiscoveryFinished")

        verify(exactly = 1) { testDeviceOne.fetchUuidsWithSdp() }
        verify(exactly = 1) { testDeviceTwo.fetchUuidsWithSdp() }

        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onUuidsFetched", testDeviceTwo, getTestUuidArrayTwo())
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onUuidsFetched", testDeviceOne, getTestUuidArrayOne())

        assertTrue(foundDevices.size == 2)
        assertTrue(foundDevices.contains(testDeviceOne) && foundDevices.contains(testDeviceTwo))
        assertTrue(foundServices.size == 2)
        assertTrue(foundServices.size == 2)
    }


    @Test
    fun itShouldPauseTheDiscoveryWhenRefreshingServices() {
        // discovered device
        SdpBluetoothDiscoveryEngine.getInstance().refreshNearbyServices()
        verify(exactly = 1) { mockedBtAdapter.cancelDiscovery() }
    }


    @Test
    fun itShouldNotifyAboutServicesThatWhereDiscoveredBefore() {

        val foundDevices: ArrayList<BluetoothDevice> = ArrayList()
        val foundServices: ArrayList<ServiceDescription> = ArrayList()
        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(object :
            BluetoothServiceDiscoveryListener {
            override fun onServiceDiscovered(
                host: BluetoothDevice,
                description: ServiceDescription
            ) {
                foundDevices.add(host)
                foundServices.add(description)
            }

            override fun onPeerDiscovered(device: BluetoothDevice) {
                // not under test here
            }

        })
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery()

        // discovered device
        val testDeviceOne = getTestDeviceOne()
        val testDeviceTwo = getTestDeviceTwo()
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceOne)
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceTwo)
        SdpBluetoothDiscoveryEngine.getInstance().callPrivateFunc("onDeviceDiscoveryFinished")

        verify(exactly = 1) { testDeviceOne.fetchUuidsWithSdp() }
        verify(exactly = 1) { testDeviceTwo.fetchUuidsWithSdp() }

        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onUuidsFetched", testDeviceTwo, getTestUuidArrayTwo())
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onUuidsFetched", testDeviceOne, getTestUuidArrayOne())

        // looking for the services at a later point
        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(testDescriptionFive)
        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(testDescriptionTwo)

        assertTrue(foundDevices.size == 2)
        assertTrue(foundDevices.contains(testDeviceOne) && foundDevices.contains(testDeviceTwo))
        assertTrue(foundServices.size == 2)
        assertTrue(foundServices.size == 2)
    }

    //
    //  ----------  frequent NullPointerException reasons ----------
    //


    /**
     * In some instances he UUID_EXTRA will be `null`
     * the engine needs to handle that without an (unexpected) exception
     *
     * This should also be caught in the BroadcastReceiver before but still
     * it should be checked
     */
    @Test
    fun itShouldHandleUuidArraysBeingNull() {
        val testDeviceOne = getTestDeviceOne()

        //Start client looking for uuid four, which is part of test array two
        val foundDevices: ArrayList<BluetoothDevice> = ArrayList()
        val foundServices: ArrayList<ServiceDescription> = ArrayList()
        SdpBluetoothDiscoveryEngine.getInstance().registerDiscoverListener(object :
            BluetoothServiceDiscoveryListener {
            override fun onServiceDiscovered(
                host: BluetoothDevice,
                description: ServiceDescription
            ) {
                foundDevices.add(host)
                foundServices.add(description)
            }

            override fun onPeerDiscovered(device: BluetoothDevice) {
                // not under test here
            }

        })

        SdpBluetoothDiscoveryEngine.getInstance().startSdpDiscoveryForService(testDescriptionTwo)
        SdpBluetoothDiscoveryEngine.getInstance().startDeviceDiscovery()
        SdpBluetoothDiscoveryEngine.getInstance()
            .callPrivateFunc("onDeviceDiscovered", testDeviceOne)
        SdpBluetoothDiscoveryEngine.getInstance().callPrivateFunc("onDeviceDiscoveryFinished")
        verify(exactly = 1) { testDeviceOne.fetchUuidsWithSdp() }

        // fetches null array uuids
        SdpBluetoothDiscoveryEngine.getInstance()
            .callOnUuidsFetchedWithNullParam(testDeviceOne, null)
        // nothing should happen , and no NullPointerException
        assertTrue(foundDevices.size == 0)
        assertTrue(foundServices.size == 0)
    }

}
