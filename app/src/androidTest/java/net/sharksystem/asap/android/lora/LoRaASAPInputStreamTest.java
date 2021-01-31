package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;
import net.sharksystem.asap.android.lora.messages.ASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.AbstractASAPLoRaMessage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.UUID;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoRaASAPInputStreamTest {

    public static LoRaBTInputOutputStream.LoRaASAPInputStream loRaASAPInputStream = new LoRaBTInputOutputStream.LoRaASAPInputStream("1000");

    @Test
    public void usesAppContext() {
        // Test if we are running in App Context
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("net.sharksystem.asap.example", appContext.getPackageName());
    }

    @Test
    public void testAvailable() throws IOException {
        //Check that stream is empty
        assertEquals(0, loRaASAPInputStream.available());

        //insert 1 Byte, check that 1 is in it
        loRaASAPInputStream.appendData("A".getBytes());
        assertEquals(1, loRaASAPInputStream.available());

        //insert random empty byte array, check that that doesn't change anything
        loRaASAPInputStream.appendData(new byte[0]);
        assertEquals(1, loRaASAPInputStream.available());

        //Insert another Byte, check that 2 are in it
        loRaASAPInputStream.appendData("B".getBytes());
        assertEquals(2, loRaASAPInputStream.available());

        //Read back the first insertion. Expect our first Byte
        String readResult = new String(new byte[]{(byte) loRaASAPInputStream.read()});
        assertEquals("A", readResult);
        assertEquals(1, loRaASAPInputStream.available());

        //Read back the second (dataful) insertion. Expect our second Byte
        readResult = new String(new byte[]{(byte) loRaASAPInputStream.read()});
        assertEquals("B", readResult);
        assertEquals(0, loRaASAPInputStream.available());

    }
}
