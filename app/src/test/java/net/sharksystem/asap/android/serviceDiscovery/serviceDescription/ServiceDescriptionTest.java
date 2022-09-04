package net.sharksystem.asap.android.serviceDiscovery.serviceDescription;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import net.sharksystem.asap.android.serviceDiscovery.ServiceDescription;

import org.junit.Test;

import java.util.HashMap;

/**
 * Unit tests for {@link ServiceDescription}
 *
 * @author WilliBoelke
 */
public class ServiceDescriptionTest
{

    @Test
    public void itShouldBeInitialized()
    {
        HashMap<String, String> serviceAttributesOne = new HashMap<>();
        serviceAttributesOne.put("service-name", "Test Service One");
        serviceAttributesOne.put("service-info", "This is a test service description");
        ServiceDescription descriptionForServiceOne = new ServiceDescription("Test Service One", serviceAttributesOne);
    }

    @Test
    public void itShouldGenerateDifferentUuidsBasedOnServiceAttributes()
    {
        HashMap<String, String> serviceAttributesOne = new HashMap<>();
        HashMap<String, String> serviceAttributesTwo = new HashMap<>();
        serviceAttributesOne.put("service-name", "Test Service One");
        serviceAttributesOne.put("service-info", "This is a test service description");
        serviceAttributesTwo.put("service-name", "Counting Service Two");
        serviceAttributesTwo.put("service-info", "This service counts upwards an sends a message containing this number to all clients");
        ServiceDescription descriptionForServiceOne = new ServiceDescription("Test Service One", serviceAttributesOne);
        ServiceDescription descriptionForServiceTwo = new ServiceDescription("Test Service Two", serviceAttributesOne);
        assertNotEquals(descriptionForServiceTwo.getServiceUuid(), descriptionForServiceOne.getServiceUuid());
    }

    @Test
    public void itShouldGenerateDifferentUuidsBasedOnServiceAttributeKeys()
    {
        HashMap<String, String> serviceAttributesOne = new HashMap<>();
        HashMap<String, String> serviceAttributesTwo = new HashMap<>();
        serviceAttributesOne.put("service-name", "Test Service One");
        serviceAttributesOne.put("service-info", "This is a test service description");
        serviceAttributesTwo.put("serviceName", "Counting Service One");
        serviceAttributesTwo.put("service-information", "This is a test service description");
        ServiceDescription descriptionForServiceOne = new ServiceDescription("Test Service One", serviceAttributesOne);
        ServiceDescription descriptionForServiceTwo = new ServiceDescription("Test Service Two", serviceAttributesOne);
        assertNotEquals(descriptionForServiceTwo.getServiceUuid(), descriptionForServiceOne.getServiceUuid());
    }


    @Test
    public void itShouldIncludeTheServiceNameInTheUuidGeneration()
    {
        HashMap<String, String> serviceAttributesOne = new HashMap<>();
        HashMap<String, String> serviceAttributesTwo = new HashMap<>();
        serviceAttributesOne.put("service-info", "This is a test service description");
        serviceAttributesTwo.put("service-info", "This is a test service description");
        ServiceDescription descriptionForServiceOne = new ServiceDescription("Test Service One", serviceAttributesOne);
        ServiceDescription descriptionForServiceTwo = new ServiceDescription("Test Service Two", serviceAttributesOne);
        assertNotEquals(descriptionForServiceTwo.getServiceUuid(), descriptionForServiceOne.getServiceUuid());
    }


    @Test
    public void twoServicesWithSameAttributesShouldBeEqual()
    {
        HashMap<String, String> serviceAttributesOne = new HashMap<>();
        HashMap<String, String> serviceAttributesTwo = new HashMap<>();
        serviceAttributesOne.put("service-name", "Test Service One");
        serviceAttributesOne.put("service-info", "This is a test service description");
        serviceAttributesTwo.put("service-name", "Test Service One");
        serviceAttributesTwo.put("service-info", "This is a test service description");
        ServiceDescription descriptionForServiceOne = new ServiceDescription("Test Service One", serviceAttributesOne);
        ServiceDescription descriptionForServiceTwo = new ServiceDescription("Test Service One", serviceAttributesOne);
        assertEquals(descriptionForServiceTwo.getServiceUuid(), descriptionForServiceOne.getServiceUuid());
    }
}