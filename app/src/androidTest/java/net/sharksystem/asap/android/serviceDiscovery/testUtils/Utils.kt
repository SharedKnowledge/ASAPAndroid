package net.sharksystem.asap.android.serviceDiscovery.testUtils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.os.Parcelable
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import net.sharksystem.asap.android.serviceDiscovery.ServiceDescription
import java.io.InputStream
import java.io.OutputStream
import java.util.*

//
//------------ get mocks -------------
//

var testUUIDOne: UUID = UUID.fromString("12345fff-f49a-11ec-b939-0242ac120002")
var testUUIDTwo: UUID = UUID.fromString("22345fff-f49a-11ec-b939-0242ac120002")
var testUUIDThree: UUID = UUID.fromString("32345fff-f49a-11ec-b939-0242ac120002")

lateinit var testDescriptionOne : ServiceDescription
lateinit var testDescriptionTwo : ServiceDescription
lateinit var testDescriptionThree : ServiceDescription
lateinit var testDescriptionFour : ServiceDescription
lateinit var testDescriptionFive : ServiceDescription

fun initTestMocks(){
    val serviceAttributesOne = HashMap<String, String>()
    val serviceAttributesTwo = HashMap<String, String>()
    val serviceAttributesTree = HashMap<String, String>()
    val serviceAttributesFour = HashMap<String, String>()
    val serviceAttributesFive = HashMap<String, String>()
    serviceAttributesOne["service-name"] = "Test Service One"
    serviceAttributesTwo["service-name"] = "Test Service Two"
    serviceAttributesTree["service-name"] = "Test Service Three"
    serviceAttributesFour["service-name"] = "Test Service Four"
    serviceAttributesFive["service-name"] = "Test Service Five"

    testDescriptionOne =
        ServiceDescription(
            "test service one",
            serviceAttributesOne
        )
    testDescriptionTwo =
        ServiceDescription(
            "test service two",
            serviceAttributesTwo
        )
    testDescriptionThree =
        ServiceDescription(
            "test service three",
            serviceAttributesTree
        )
    testDescriptionFour =
        ServiceDescription(
            "test service four",
            serviceAttributesFour
        )
    testDescriptionFive =
        ServiceDescription(
            "test service five",
            serviceAttributesFive
        )

    testDescriptionOne.overrideUuidForBluetooth(testUUIDOne)
    testDescriptionTwo.overrideUuidForBluetooth(testUUIDTwo)
    testDescriptionThree.overrideUuidForBluetooth(testUUIDThree)
}

fun getTestDeviceOne(): BluetoothDevice {
    val deviceOne = mockk<BluetoothDevice>()
    every { deviceOne.fetchUuidsWithSdp() } returns true
    every { deviceOne.name } returns  "testDeviceOneName"
    every { deviceOne.address } returns  "testDeviceOneAddress"
    every { deviceOne.uuids } returns  arrayOf(
        ParcelUuid(testUUIDTwo),
        ParcelUuid(testUUIDThree)
    )

    return deviceOne
}

fun getTestDeviceTwo(): BluetoothDevice {
    val deviceTwo = mockk<BluetoothDevice>()
    every { deviceTwo.fetchUuidsWithSdp() } returns true
    every { deviceTwo.name } returns  "testDeviceTwoName"
    every { deviceTwo.address } returns  "testDeviceTwoAddress"
    every { deviceTwo.uuids } returns  arrayOf(
        ParcelUuid(testDescriptionFour.serviceUuid),
        ParcelUuid(testDescriptionFive.serviceUuid)
    )
    return deviceTwo
}

fun getTestUuidArrayOne(): Array<Parcelable> {
    val parcelUuidTwo = ParcelUuid(testUUIDTwo)
    val parcelUuidThree = ParcelUuid(testUUIDThree)
    return arrayOf(parcelUuidTwo, parcelUuidThree)
}

fun getTestUuidArrayTwo(): Array<Parcelable> {
    val parcelUuidFour = ParcelUuid(testDescriptionFour.serviceUuid)
    val parcelUuidFive = ParcelUuid(testDescriptionFive.serviceUuid)
    return arrayOf(parcelUuidFour, parcelUuidFive)
}


fun getSocketToTestDevice(device: BluetoothDevice): BluetoothSocket {
    val mockedSocket = mockk<BluetoothSocket>()
    val outputStream = mockk<OutputStream>(relaxed = true)
    val inputStream = mockk<InputStream>(relaxed = true)
    justRun { outputStream.close() }
    justRun { inputStream.close() }
    every {mockedSocket.inputStream} returns  inputStream
    every {mockedSocket.outputStream} returns  outputStream
    every {mockedSocket.remoteDevice} returns  device
    justRun {mockedSocket.close()}
    return mockedSocket
}



//
//------------ utils -------------
//

/**
 * Kotlin (unlike Java) dies not allow calling protected methods
 * while being inside the same package.
 *
 * For testing the engine with a mocked Android API i need access
 * to the methods called by the broadcast receivers:
 *
 * `onDeviceDiscovered`, `onUuidsFetched` and  `onDiscoveryFinished`
 *
 * There where two possibilities, either sending an intent to the broadcast receivers
 * or accessing the protected methods using reflections.
 *
 * I couldn't quite work out the intent version and i think it is not possible to send
 * the listened intents since sending those could cause some troubles in the system
 *
 * so i went with this (a lille hacky) version. but it is for testing and it would have
 * been allowed in Java anyways so i think its fair.
 * Better then making the methods .
 *
 * Also partially stolen from here, since i am not too familiar with kotlin:
 * {@link https://stackoverflow.com/questions/48158909/java-android-kotlin-reflection-on--field-and-call--methods-on-it}
 */
inline fun <reified T> T.callPrivateFunc(name: String, vararg args: Any?): Any? {
    val classArray: Array<Class<*>> = args.map { it!!::class.java}.toTypedArray()
    return T::class.java.getDeclaredMethod(name, *classArray)
        .apply { isAccessible = true }
        .invoke(this, *args)
}


/**
 * This a specialized version of the general callPrivateFun method.
 * It only calls "onUuidsFetched" but allows parameters to be null
 */
inline fun <reified T> T.callOnUuidsFetchedWithNullParam(vararg args: Any?): Any? {
    return T::class.java.getDeclaredMethod("onUuidsFetched", BluetoothDevice::class.java, Array<Parcelable>::class.java)
        .apply { isAccessible = true }
        .invoke(this, *args)
}
