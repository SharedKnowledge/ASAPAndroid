package net.sharksystem.asap.android.LoRaEngine;


import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Base64Test {

    @Test
    public void usesAppContext() {
        // Test if we are running in App Context
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("net.sharksystem.asap.example", appContext.getPackageName());
    }

    @Test
    public void charsetEncodeDecode() {
        byte[] message = "Testmessage123!&".getBytes();
        String base64message = new String(Base64.getEncoder().encode(message), StandardCharsets.UTF_8);
        byte[] decodedMessage = Base64.getMimeDecoder().decode(base64message.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(message, decodedMessage);
        assertEquals("Testmessage123!&", new String(decodedMessage));
    }

    @Test
    public void simpleEncodeDecode() {
        byte[] message = "Testmessage123!&".getBytes();
        String base64message = Base64.getEncoder().encodeToString(message);
        assertEquals("VGVzdG1lc3NhZ2UxMjMhJg==", base64message);
        byte[] decodedMessage = Base64.getMimeDecoder().decode(base64message);
        assertArrayEquals(message, decodedMessage);
        assertEquals("Testmessage123!&", new String(decodedMessage));
    }
}