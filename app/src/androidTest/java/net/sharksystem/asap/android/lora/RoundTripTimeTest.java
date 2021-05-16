package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RoundTripTimeTest {

    public static String LONGEST_MESSAGE = "This is exactly 174 Characters long. This is exactly 174 Characters long. This is exactly 174 Characters long. This is exactly 174 Characters long. This is exactly 174 Charact";

    public static BluetoothDevice Alice;
    public static BluetoothDevice Bob;
    public static BluetoothSocket AliceSocket;
    public static BluetoothSocket BobSocket;

    @BeforeClass
    public static void setup() throws IOException, InterruptedException {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.cancelDiscovery();

        for (BluetoothDevice btDevice : btAdapter.getBondedDevices()) {
            if (btDevice.getName().indexOf("ASAP-LoRa-1") == 0) {
                RoundTripTimeTest.Alice = btDevice;
            }
            if (btDevice.getName().indexOf("ASAP-LoRa-2") == 0) {
                RoundTripTimeTest.Bob = btDevice;
            }
        }
        if (RoundTripTimeTest.Alice == null || RoundTripTimeTest.Bob == null)
            throw new IOException("Please Pair BT Modules ASAP-LoRa-1 and ASAP-LoRa-2 to this device!");

        RoundTripTimeTest.AliceSocket = RoundTripTimeTest.Alice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        RoundTripTimeTest.BobSocket = RoundTripTimeTest.Bob.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

        RoundTripTimeTest.AliceSocket.connect();
        RoundTripTimeTest.BobSocket.connect();

        Thread.sleep(2000); //Give the BT Modules some time to stabilize
    }

    @AfterClass
    public static void teardown() throws InterruptedException, IOException {
        RoundTripTimeTest.AliceSocket.close();
        RoundTripTimeTest.BobSocket.close();
        Thread.sleep(2000); //Give the BT Modules some time to stabilize
    }
    @Test
    public void usesAppContext() {
        // Test if we are running in App Context
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("net.sharksystem.asap.example", appContext.getPackageName());
    }

    @Test(timeout = 30000)
    public void checkLongestMessageTime() throws IOException {
        this.AliceSocket.getOutputStream().write(("MSSGE:1001:"+LONGEST_MESSAGE+"\n").getBytes());
        this.AliceSocket.getOutputStream().flush();

        while (true) {
            if (this.BobSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.BobSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals(("MSSGE:1000:"+LONGEST_MESSAGE), deviceResponse);
                break;
            }
        }
    }

    @Test(timeout = 380000)
    public void simultaneousLongMessageTest() throws IOException {
        this.BobSocket.getOutputStream().write(("MSSGE:1000:"+LONGEST_MESSAGE+"\n").getBytes());
        this.AliceSocket.getOutputStream().write(("MSSGE:1001:"+LONGEST_MESSAGE+"\n").getBytes());
        this.BobSocket.getOutputStream().flush();
        this.AliceSocket.getOutputStream().flush();

        while (true) {
            if (this.BobSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.BobSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("MSSGE:1000:"+LONGEST_MESSAGE, deviceResponse);
                break;
            }
        }

        while (true) {
            if (this.AliceSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.AliceSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("MSSGE:1001:"+LONGEST_MESSAGE, deviceResponse);
                break;
            }
        }
    }

    @Test(timeout = 380000)
    public void simultaneousLongMessageTest2() throws IOException {
        simultaneousLongMessageTest();
    }
    @Test(timeout = 380000)
    public void simultaneousLongMessageTest3() throws IOException {
        simultaneousLongMessageTest();
    }
    @Test(timeout = 380000)
    public void simultaneousLongMessageTest4() throws IOException {
        simultaneousLongMessageTest();
    }
    @Test(timeout = 380000)
    public void simultaneousLongMessageTest5() throws IOException {
        simultaneousLongMessageTest();
    }

}