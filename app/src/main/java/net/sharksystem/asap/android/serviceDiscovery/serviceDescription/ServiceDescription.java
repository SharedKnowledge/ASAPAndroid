package net.sharksystem.asap.android.serviceDiscovery.serviceDescription;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Describes a service.
 *
 * <p>
 * Service Descriptions<br>
 * ------------------------------------------------------------<br>
 * A service can be described through a number of attributes.
 * Attributes are in a key-value format.
 * <p>
 * A simplified and shorter version to distinct service from
 * each other is through the USe of unique IDs in the for of
 * UUIDs.
 * This class combines both methods, attributes can be added
 * from the attributes either a map (serving as a record of the
 * service attributes) or a UUID can be generated.
 *
 * <p>
 * Custom UUID<br>
 * ------------------------------------------------------------<br>
 * A custom UUID can be set using {@link #overrideUuidForBluetooth(UUID)},
 * this is for the use with Bluetooth and cant be used for Wifi Direct.
 * Wifi-Direct will use the full Service Records and not the UUID.
 * Thus the UUID cant be resolved at the discovering side.
 * <p>
 * Equality<br>
 * ------------------------------------------------------------<br>
 * For service descriptions to be equal the service UUID needs to be the same
 * This is the only attribute that will be compared in {@link #equals(Object)}
 *
 * @author WilliBoelke
 */
public class ServiceDescription
{
    //
    //  ----------  instance variables ----------
    //

    /**
     * Classname for logging
     */
    private final String TAG = this.getClass().getSimpleName();

    /**
     * The Service attributes
     */
    private final Map<String, String> attributes;

    /**
     * A custom uuid, which - when set- will be used
     * instead of a generated one
     */
    private UUID serviceUuid;


    //
    //  ---------- constructor and initialization ----------
    //

    /**
     * Public constructor
     *
     * @param serviceRecord
     *         The service record, this needs to contain at least one
     *         key - value
     */
    public ServiceDescription(Map<String, String> serviceRecord)
    {
        this.attributes = serviceRecord;
    }

    /**
     * A UUID set using this overrides the UUID generated from teh service records,
     * making it and the service records independent from each other.
     *
     * <p>
     * NOTE<br>
     * ------------------------------------------------------------<br>
     * ...that this only work for the BluetoothService discovery and wont
     * work with WifiDirect
     * This is based on WiFi direct exchanging the service records,
     * while Bluetooth will exchange the UUID itself.
     *
     * @param uuid
     *         A custom UUId overriding the one generated from the Service records
     */
    public void overrideUuidForBluetooth(UUID uuid)
    {
        // Todo maybe a more elegant solution can be found,
        // it should be worth it to make wifi direct exchanging the UUID
        // when a custom UUID has been set, the custom UUID though should stay, so Bluetooth service
        // which don't use a UUID based on these service records can be found.
        // For now - its here in the comment, hope this wil be seen
        this.serviceUuid = uuid;
    }

    /**
     * Returns either the UUID set through {@link #overrideUuidForBluetooth(UUID)}
     * or a UUID generated from the services attributes.
     *
     * @return the services UUID
     *
     * @throws NullPointerException
     *         IF the attributes and the custom UUID are null
     */
    public UUID getServiceUuid()
    {
        if (this.serviceUuid == null)
        {
            //--- generating UUID from attributes ---//
            this.serviceUuid = getUuidForServiceRecord(this.attributes);
        }

        return this.serviceUuid;
    }

    /**
     * Returns the Service records ad a Map object.
     * The map containing all key value pairs set through
     *
     * @return The service records Map
     */
    public Map<String, String> getServiceRecord()
    {
        return this.attributes;
    }

    /**
     * Generates a deterministic UUID from a Map (service records)
     *
     * @param serviceRecord
     *         A Map containing key value pairs, describing a service
     *
     * @return A UUID generated from the map entries
     *
     * @throws NullPointerException
     *         If the given Map was empty
     */
    public static UUID getUuidForServiceRecord(Map<String, String> serviceRecord) throws NullPointerException
    {
        if (serviceRecord.size() == 0)
        {
            throw new NullPointerException("There are no service attributes specified");
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : serviceRecord.entrySet())
        {
            sb.append(entry.getKey());
            sb.append(entry.getValue());
        }
        return UUID.nameUUIDFromBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
    }


    /**
     * This method reverses a UUID Bytewise
     * <p>
     * This is a workaround for a problem which causes UUIDs
     * obtained with `fetchUuidsWithSdp()` to be in a little endian format
     * This problem is apparently not specific to a certain android version
     * since it only occurred on one of my devices running Android 8.1, the other one
     * (with the same version) didn't had this problem.
     * <p>
     * This will be used on every discovered UUID when enabled in the
     * SdpBluetoothEngine`s configuration, sine the problem cant be predetermined
     * by any means i found.
     * <p>
     * References<br>
     * ------------------------------------------------------------<br>
     * This Problem is mentioned in the the google Issue tracker
     * <a href="https://issuetracker.google.com/issues/37075233">...</a>
     * The code used here to reverse the UUID is stolen from the issues comments and can be found here
     * <a href="https://gist.github.com/masterjefferson/10922165432ec016a823e46c6eb382e6">...</a>
     *
     * @return the bytewise revered uuid
     */
    public UUID getBytewiseReverseUuid()
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer
                .putLong(this.serviceUuid.getLeastSignificantBits())
                .putLong(this.serviceUuid.getMostSignificantBits());
        byteBuffer.rewind();
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        ServiceDescription that = (ServiceDescription) o;
        return this.getServiceUuid().equals(that.getServiceUuid());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.getServiceUuid());
    }

    @NonNull
    @Override
    public String toString()
    {
        StringBuilder sd = new StringBuilder();
        sd.append("Service: { ");
        sd.append("\nUuid = ");
        sd.append(this.getServiceUuid());
        sd.append(",");
        sd.append("\n Attributes =  " + this.attributes.toString());
        return sd.toString();
    }
}
