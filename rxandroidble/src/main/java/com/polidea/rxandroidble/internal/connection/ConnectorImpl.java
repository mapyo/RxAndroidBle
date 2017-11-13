package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.ConnectionSetup;
import com.polidea.rxandroidble.internal.serialization.ClientOperationQueue;

import java.util.Set;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import rx.functions.Func0;

public class ConnectorImpl implements Connector {

    private final ClientOperationQueue clientOperationQueue;
    private final ConnectionComponent.Builder connectionComponentBuilder;

    @Inject
    public ConnectorImpl(
            ClientOperationQueue clientOperationQueue,
            ConnectionComponent.Builder connectionComponentBuilder) {
        this.clientOperationQueue = clientOperationQueue;
        this.connectionComponentBuilder = connectionComponentBuilder;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(final ConnectionSetup options) {
        return Observable.defer(new Func0<Observable<RxBleConnection>>() {
            @Override
            public Observable<RxBleConnection> call() {

                final ConnectionComponent connectionComponent = connectionComponentBuilder
                        .connectionModule(new ConnectionModule(options))
                        .build();

                final Observable<RxBleConnection> newConnectionObservable = Observable.fromCallable(new Callable<RxBleConnection>() {
                    @Override
                    public RxBleConnection call() throws Exception {
                        // BluetoothGatt is needed for RxBleConnection
                        // BluetoothGatt is produced by RxBleRadioOperationConnect
                        return connectionComponent.rxBleConnection();
                    }
                });
                final Observable<BluetoothGatt> connectedObservable = clientOperationQueue.queue(connectionComponent.connectOperation());
                final Observable<RxBleConnection> disconnectedErrorObservable = connectionComponent.gattCallback().observeDisconnect();
                final Set<ConnectionSubscriptionWatcher> connSubWatchers = connectionComponent.connectionSubscriptionWatchers();

                return Observable.merge(
                        newConnectionObservable.delaySubscription(connectedObservable),
                        disconnectedErrorObservable
                )
                        .doOnSubscribe(new Action0() {
                            @Override
                            public void call() {
                                for (ConnectionSubscriptionWatcher csa : connSubWatchers) {
                                    csa.onConnectionSubscribed();
                                }
                            }
                        })
                        .doOnUnsubscribe(new Action0() {
                            @Override
                            public void call() {
                                for (ConnectionSubscriptionWatcher csa : connSubWatchers) {
                                    csa.onConnectionUnsubscribed();
                                }
                            }
                        });
            }
        });
    }
}
