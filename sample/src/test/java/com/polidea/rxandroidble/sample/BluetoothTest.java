package com.polidea.rxandroidble.sample;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.mockrxandroidble.RxBleClientMock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.UUID;

import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

@RunWith(RobolectricTestRunner.class)
public class BluetoothTest {
    private final static String MOCK_MAC_ADDRESS = "12:AB:34:BC:56:DE";

    private final static UUID MESSAGE_CHARACTERISTIC_UUID =
            UUID.fromString("a5648e0a-7860-11e7-b5a5-be2e44b06b34");

    private final static UUID SERVICE_CHARACTERISTIC_UUID =
            UUID.fromString("32cc3f90-7861-11e7-b5a5-be2e44b06b34");

    private RxBleClient bleClient;

    @Before
    public void setupMockClient() {
        bleClient = new RxBleClientMock.Builder().addDevice(buildMockDevice()).build();
    }

    @Test
    public void testMessageSmallerThanMtu() {
        TestSubscriber<byte[]> messageIndicationSubscriber = TestSubscriber.create();

        bleClient.getBleDevice(MOCK_MAC_ADDRESS)
                .establishConnection(false)
                .flatMap(new Func1<RxBleConnection, Observable<byte[]>>() {
                    @Override public Observable<byte[]> call(RxBleConnection rxBleConnection) {

                        byte[] message = { 0x07, 0x13, 0x05, 0x00, 0x00 };

                        return rxBleConnection.writeCharacteristic(MESSAGE_CHARACTERISTIC_UUID,
                                message);
                    }
                })
                .subscribe(messageIndicationSubscriber);

        messageIndicationSubscriber.assertNoErrors();
        messageIndicationSubscriber.assertValueCount(1);
    }

    private RxBleDevice buildMockDevice() {

        return new RxBleClientMock.DeviceBuilder().rssi(90)
                .deviceMacAddress(MOCK_MAC_ADDRESS)
                .deviceName("BLEDevice")
                .scanRecord(new byte[] { 0, 1, 2, 3 })
                .notificationSource(MESSAGE_CHARACTERISTIC_UUID,
                        Observable.just(new byte[] { 2, 4, 6, 8 }))
                .addService(SERVICE_CHARACTERISTIC_UUID, buildCharacteristics())
                .build();

    }

    private List<BluetoothGattCharacteristic> buildCharacteristics() {

        RxBleClientMock.DescriptorsBuilder descriptorsBuilder =
                new RxBleClientMock.DescriptorsBuilder().addDescriptor(UUID.randomUUID(),
                        new byte[] { 0x01, 0x02, 0x03 });

        return new RxBleClientMock.CharacteristicsBuilder().addCharacteristic(
                MESSAGE_CHARACTERISTIC_UUID, new byte[0], descriptorsBuilder.build()).build();
    }
}
