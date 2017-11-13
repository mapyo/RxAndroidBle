package com.polidea.rxandroidble.helpers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.DaggerClientComponent;
import com.polidea.rxandroidble.internal.util.LocationServicesStatus;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import rx.functions.Cancellable;
import rx.internal.operators.OnSubscribeCreate;

/**
 * An Observable that emits false if an attempt to scan with {@link com.polidea.rxandroidble.RxBleClient#scanBleDevices(UUID...)}
 * would cause the exception {@link com.polidea.rxandroidble.exceptions.BleScanException#LOCATION_SERVICES_DISABLED}; otherwise emits true.
 * Always emits true in Android versions prior to 6.0.
 * Typically, receiving false should cause the user to be prompted to enable Location Services.
 */
public class LocationServicesOkObservable extends Observable<Boolean> {

    public static LocationServicesOkObservable createInstance(@NonNull final Context context) {
        return DaggerClientComponent
                .builder()
                .clientModule(new ClientComponent.ClientModule(context))
                .build()
                .locationServicesOkObservable();
    }

    @Inject
    LocationServicesOkObservable(@NonNull final Context context, @NonNull final LocationServicesStatus locationServicesStatus) {
        super(new OnSubscribeCreate<>(
                new Action1<Emitter<Boolean>>() {
                    @Override
                    public void call(final Emitter<Boolean> emitter) {
                        final boolean locationProviderOk = locationServicesStatus.isLocationProviderOk();
                        final AtomicBoolean locationProviderOkAtomicBoolean = new AtomicBoolean(locationProviderOk);
                        emitter.onNext(locationProviderOk);

                        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                final boolean newLocationProviderOkValue = locationServicesStatus.isLocationProviderOk();
                                final boolean valueChanged = locationProviderOkAtomicBoolean
                                        .compareAndSet(!newLocationProviderOkValue, newLocationProviderOkValue);
                                if (valueChanged) {
                                    emitter.onNext(newLocationProviderOkValue);
                                }
                            }
                        };

                        context.registerReceiver(broadcastReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
                        emitter.setCancellation(new Cancellable() {
                            @Override
                            public void cancel() throws Exception {
                                context.unregisterReceiver(broadcastReceiver);
                            }
                        });
                    }
                },
                Emitter.BackpressureMode.LATEST
        ));
    }
}
