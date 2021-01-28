package net.sharksystem.asap.android.lora;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaMessageException;
import net.sharksystem.asap.android.lora.messages.ASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.AbstractASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.DeviceDiscoveredASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.DiscoverASAPLoRaMessage;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AbstractASAPLoRaMessageFactoryTest {

    @Test
    public void ASAPLoRaMessageTest() throws ASAPLoRaMessageException {
        AbstractASAPLoRaMessage factoryMessage = AbstractASAPLoRaMessage.createASAPLoRaMessage("MSSGE@1000:QWJzdHJhY3RBU0FQTG9SYU1lc3NhZ2VGYWN0b3J5VGVzdA==");
        assertTrue(factoryMessage instanceof ASAPLoRaMessage);
        assertEquals(new ASAPLoRaMessage("1000", "AbstractASAPLoRaMessageFactoryTest".getBytes()).toString(), factoryMessage.toString());
    }

    @Test
    public void DeviceDiscoveredASAPLoRaMessage() throws ASAPLoRaMessageException {
        AbstractASAPLoRaMessage factoryMessage = AbstractASAPLoRaMessage.createASAPLoRaMessage("DVDCR:1001");
        assertTrue(factoryMessage instanceof DeviceDiscoveredASAPLoRaMessage);
        assertEquals((new DeviceDiscoveredASAPLoRaMessage("1001")).toString(), factoryMessage.toString());
    }

    @Test(expected = ASAPLoRaMessageException.class)
    public void InvalidFormatTest() throws ASAPLoRaMessageException {
        AbstractASAPLoRaMessage.createASAPLoRaMessage("ThisIsNotValid");
    }

    @Test(expected = ASAPLoRaMessageException.class)
    public void EmptyMessageTest() throws ASAPLoRaMessageException {
        AbstractASAPLoRaMessage.createASAPLoRaMessage("");
    }

    @Test(expected = ASAPLoRaMessageException.class)
    public void TooShortMessageTest() throws ASAPLoRaMessageException {
        AbstractASAPLoRaMessage.createASAPLoRaMessage("5LTRS");
    }
}