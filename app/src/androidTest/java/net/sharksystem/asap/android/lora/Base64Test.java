package net.sharksystem.asap.android.lora;


import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import android.util.Base64;

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
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("net.sharksystem.asap.example", appContext.getPackageName());
    }

    @Test
    public void simpleEncodeDecode() {
        byte[] message = "Testmessage123!&".getBytes();
        String base64message = Base64.encodeToString(message, Base64.DEFAULT);
        byte[] decodedMessage = Base64.decode(base64message, Base64.DEFAULT);
        assertArrayEquals(message, decodedMessage);
        assertEquals("Testmessage123!&", new String(decodedMessage));
    }
}