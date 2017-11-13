package com.polidea.rxandroidble.internal.connection;


import com.polidea.rxandroidble.internal.ConnectionSetup;
import com.polidea.rxandroidble.RxBleConnection;
import io.reactivex.Observable;

public interface Connector {

    Observable<RxBleConnection> prepareConnection(ConnectionSetup autoConnect);
}
