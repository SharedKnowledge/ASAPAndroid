package net.sharksystem.asap.android.lora;

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
public class SimultaneousCommunicationTest {

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
                SimultaneousCommunicationTest.Alice = btDevice;
            }
            if (btDevice.getName().indexOf("ASAP-LoRa-2") == 0) {
                SimultaneousCommunicationTest.Bob = btDevice;
            }
        }
        if (SimultaneousCommunicationTest.Alice == null || SimultaneousCommunicationTest.Bob == null)
            throw new IOException("Please Pair BT Modules ASAP-LoRa-1 and ASAP-LoRa-2 to this device!");

        SimultaneousCommunicationTest.AliceSocket = SimultaneousCommunicationTest.Alice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        SimultaneousCommunicationTest.BobSocket = SimultaneousCommunicationTest.Bob.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

        SimultaneousCommunicationTest.AliceSocket.connect();
        SimultaneousCommunicationTest.BobSocket.connect();

        Thread.sleep(2000); //Give the BT Modules some time to stabilize
    }

    @Test
    public void usesAppContext() {
        // Test if we are running in App Context
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("net.sharksystem.asap.example", appContext.getPackageName());
    }

    @Test(timeout = 60000)
    public void simultaneousMessageTest() throws IOException {
        this.BobSocket.getOutputStream().write("MSSGE@1000:Simultan Hello Alice!\n".getBytes());
        this.AliceSocket.getOutputStream().write("MSSGE@1001:Simultan Hello Bob!\n".getBytes());

        while (true) {
            if (this.BobSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.BobSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("MSSGE@1000:Simultan Hello Bob!", deviceResponse);
                break;
            }
        }

        while (true) {
            if (this.AliceSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.AliceSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("MSSGE@1001:Simultan Hello Alice!", deviceResponse);
                break;
            }
        }
    }
/*
    @Test(timeout = 240000)
    public void tenSimultaneousMessageTest() throws IOException {
        int rounds = 10;
        for (int i = 0; i < rounds; i++) {
            this.BobSocket.getOutputStream().write("MSSGE@1000:Simultan Hello Alice!\n".getBytes());
            this.AliceSocket.getOutputStream().write("MSSGE@1001:Simultan Hello Bob!\n".getBytes());
        }

        int AliceCounter = 0;
        int BobCounter = 0;
        while (true) {
            if (this.AliceSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.AliceSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("MSSGE@1001:Simultan Hello Alice!", deviceResponse);
                AliceCounter++;
                System.out.print("Found that much correct answers from Bob: ");
                System.out.println(AliceCounter);
            }
            if (this.BobSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.BobSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("MSSGE@1000:Simultan Hello Bob!", deviceResponse);
                BobCounter++;
                System.out.print("Found that much correct answers from Alice: ");
                System.out.println(BobCounter);
            }
            if (AliceCounter == rounds && BobCounter == rounds) break;
        }
        assertEquals(rounds, AliceCounter);
        assertEquals(rounds, BobCounter);
    }
*/
    @Test(timeout = 240000)
    public void tenSimultaneousOrderedMessageTest() throws IOException {
        int rounds = 10;
        for (int i = 0; i < rounds; i++) {
            this.BobSocket.getOutputStream().write(("MSSGE@1000:Hello Alice Nr. " + String.valueOf(i) + "\n").getBytes());
            this.AliceSocket.getOutputStream().write(("MSSGE@1001:Hello Bob Nr. " + String.valueOf(i) + "\n").getBytes());
        }

        int AliceCounter = 0;
        int BobCounter = 0;
        while (AliceCounter < rounds || BobCounter < rounds) {
            if (this.AliceSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.AliceSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("MSSGE@1001:Hello Alice Nr. " + String.valueOf(AliceCounter), deviceResponse);
                AliceCounter++;
                System.out.print("Found that much correct answers from Bob: ");
                System.out.println(AliceCounter);
            }
            if (this.BobSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.BobSocket.getInputStream()));
                String deviceResponse = br.readLine().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("MSSGE@1000:Hello Bob Nr. " + String.valueOf(BobCounter), deviceResponse);
                BobCounter++;
                System.out.print("Found that much correct answers from Alice: ");
                System.out.println(BobCounter);
            }
        }
        assertEquals(rounds, AliceCounter);
        assertEquals(rounds, BobCounter);
    }
}