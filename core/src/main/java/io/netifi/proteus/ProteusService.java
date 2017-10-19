package io.netifi.proteus;

import io.rsocket.RSocket;

public interface ProteusService extends RSocket {
    int getPackageId();
    int getServiceId();
}
