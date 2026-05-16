package org.eclipse.californium.elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/PersistentConnector.class */
public interface PersistentConnector {
    int saveConnections(OutputStream outputStream, long j) throws IOException;

    int loadConnections(InputStream inputStream, long j) throws IOException;
}
