package net.sharksystem.asap.android.LoRaEngine;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import net.sharksystem.asap.android.lora.ASAPLoRaException;
import net.sharksystem.asap.android.lora.LoRaBTInputOutputStream;
import net.sharksystem.asap.android.lora.messages.DiscoverASAPLoRaMessage;

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

        Thread.sleep(5000); //Give the BT Modules some time to stabilize
    }

    @Test
    public void usesAppContext() {
        // Test if we are running in App Context
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("net.sharksystem.asap.example", appContext.getPackageName());
    }

    @Test(timeout=20000)
    public void deviceDiscoveryTest() throws IOException {
        this.AliceSocket.getOutputStream().write("{\"COMMAND\":\".DiscoverASAPLoRaMessage\"}".getBytes());

        while(true){
            if(this.BobSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.BobSocket.getInputStream()));
                StringBuilder sb = new StringBuilder(this.BobSocket.getInputStream().available());
                do {
                    sb.append(br.readLine()).append("\n");
                } while(br.ready());
                String deviceResponse = sb.toString().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("{\"COMMAND\":\".DeviceDiscoveredASAPLoRaMessage\",\"address\":\"1000\"}", deviceResponse);
                break;
            }
        }

        while(true){
            if(this.AliceSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.AliceSocket.getInputStream()));
                StringBuilder sb = new StringBuilder(this.AliceSocket.getInputStream().available());
                do {
                    sb.append(br.readLine()).append("\n");
                } while(br.ready());
                String deviceResponse = sb.toString().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("{\"COMMAND\":\".DeviceDiscoveredASAPLoRaMessage\",\"address\":\"1001\"}", deviceResponse);
                break;
            }
        }
    }

    @Test(timeout=10000)
    public void simpleAliceToBobMessageTest() throws IOException {
        this.AliceSocket.getOutputStream().write("{\"COMMAND\":\".ASAPLoRaMessage\",\"address\":\"1001\",\"message\":\"Hello World!\"}".getBytes());

        while(true){
            if(this.BobSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.BobSocket.getInputStream()));
                StringBuilder sb = new StringBuilder(this.BobSocket.getInputStream().available());
                do {
                    sb.append(br.readLine()).append("\n");
                } while(br.ready());
                String deviceResponse = sb.toString().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("{\"COMMAND\":\".ASAPLoRaMessage\",\"address\":\"1000\",\"message\":\"Hello World!\"}", deviceResponse);
                break;
            }
        }
    }

    @Test(timeout=10000)
    public void simpleBobToAliceMessageTest() throws IOException {
        this.BobSocket.getOutputStream().write("{\"COMMAND\":\".ASAPLoRaMessage\",\"address\":\"1000\",\"message\":\"Hello World!\"}".getBytes());

        while(true){
            if(this.AliceSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.AliceSocket.getInputStream()));
                StringBuilder sb = new StringBuilder(this.AliceSocket.getInputStream().available());
                do {
                    sb.append(br.readLine()).append("\n");
                } while(br.ready());
                String deviceResponse = sb.toString().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("{\"COMMAND\":\".ASAPLoRaMessage\",\"address\":\"1001\",\"message\":\"Hello World!\"}", deviceResponse);
                break;
            }
        }
    }

    @Test(timeout=20000)
    public void simultaneousMessageTest() throws IOException {
        this.BobSocket.getOutputStream().write("{\"COMMAND\":\".ASAPLoRaMessage\",\"address\":\"1000\",\"message\":\"Hello World!\"}".getBytes());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.AliceSocket.getOutputStream().write("{\"COMMAND\":\".ASAPLoRaMessage\",\"address\":\"1001\",\"message\":\"Hello World!\"}".getBytes());

        while(true){
            if(this.BobSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.BobSocket.getInputStream()));
                StringBuilder sb = new StringBuilder(this.BobSocket.getInputStream().available());
                do {
                    sb.append(br.readLine()).append("\n");
                } while(br.ready());
                String deviceResponse = sb.toString().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("{\"COMMAND\":\".ASAPLoRaMessage\",\"address\":\"1000\",\"message\":\"Hello World!\"}", deviceResponse);
                break;
            }
        }

        while(true){
            if(this.AliceSocket.getInputStream().available() > 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.AliceSocket.getInputStream()));
                StringBuilder sb = new StringBuilder(this.AliceSocket.getInputStream().available());
                do {
                    sb.append(br.readLine()).append("\n");
                } while(br.ready());
                String deviceResponse = sb.toString().trim();
                System.out.print("ASAP LoRaEngine Test Device Response: ");
                System.out.println(deviceResponse);
                assertEquals("{\"COMMAND\":\".ASAPLoRaMessage\",\"address\":\"1001\",\"message\":\"Hello World!\"}", deviceResponse);
                break;
            }
        }
    }
}