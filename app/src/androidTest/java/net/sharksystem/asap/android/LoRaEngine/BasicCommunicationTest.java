package net.sharksystem.asap.android.LoRaEngine;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicCommunicationTest {

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
                BasicCommunicationTest.Alice = btDevice;
            }
            if (btDevice.getName().indexOf("ASAP-LoRa-2") == 0) {
                BasicCommunicationTest.Bob = btDevice;
            }
        }
        if (BasicCommunicationTest.Alice == null || BasicCommunicationTest.Bob == null)
            throw new IOException("Please Pair BT Modules ASAP-LoRa-1 and ASAP-LoRa-2 to this device!");

        BasicCommunicationTest.AliceSocket = BasicCommunicationTest.Alice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        BasicCommunicationTest.BobSocket = BasicCommunicationTest.Bob.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

        BasicCommunicationTest.AliceSocket.connect();
        BasicCommunicationTest.BobSocket.connect();

        Thread.sleep(2000); //Give the BT Modules some time to stabilize
    }

    @Test
    public void usesAppContext() {
        // Test if we are running in App Context
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("net.sharksystem.asap.example", appContext.getPackageName());
    }

    @Test(timeout = 30000)
    public void deviceDiscoveryTest() throws IOException {
        this.AliceSocket.getOutputStream().write("DSCVR\n".getBytes());

        while (true) {
            if (this.BobSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.BobSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("DVDCR:1000", deviceResponse);
                break;
            }
        }

        while (true) {
            if (this.AliceSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.AliceSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("DVDCR:1001", deviceResponse);
                break;
            }
        }
    }

    @Test(timeout = 30000)
    public void simpleAliceToBobMessageTest() throws IOException {
        this.AliceSocket.getOutputStream().write("MSSGE@1001:Hello World!\n".getBytes());

        while (true) {
            if (this.BobSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.BobSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("MSSGE@1000:Hello World!", deviceResponse);
                break;
            }
        }
    }

    @Test(timeout = 30000)
    public void simpleBobToAliceMessageTest() throws IOException {
        this.BobSocket.getOutputStream().write("MSSGE@1000:Hello World!".getBytes());

        while (true) {
            if (this.AliceSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.AliceSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("MSSGE@1001:Hello World!", deviceResponse);
                break;
            }
        }
    }
}