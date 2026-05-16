package org.eclipse.californium.core.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.elements.util.DataStreamReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.eclipse.californium.elements.util.SerializationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/ServersSerializationUtil.class */
public class ServersSerializationUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServersSerializationUtil.class);

    public static int loadServers(InputStream in, CoapServer... servers) {
        return loadServers(in, (List<CoapServer>) Arrays.asList(servers));
    }

    public static int loadServers(InputStream in, List<CoapServer> servers) {
        int count = 0;
        long time = System.nanoTime();
        try {
            DataStreamReader reader = new DataStreamReader(in);
            long delta = SerializationUtil.readNanotimeSynchronizationMark(reader);
            while (true) {
                CoapServer.ConnectorIdentifier id = CoapServer.readConnectorIdentifier(in);
                if (id == null) {
                    break;
                }
                boolean foundTag = false;
                int loaded = -1;
                Iterator<CoapServer> it = servers.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    CoapServer server = it.next();
                    if (id.tag.equals(server.getTag())) {
                        foundTag = true;
                        loaded = server.loadConnector(id, in, delta);
                        if (loaded >= 0) {
                            count += loaded;
                            break;
                        }
                    }
                }
                if (foundTag) {
                    if (loaded < 0) {
                        LOGGER.warn("{}loading {} failed, no connector in {} servers!", new Object[]{id.tag, id.uri, Integer.valueOf(servers.size())});
                        SerializationUtil.skipItems(in, 16);
                    } else {
                        LOGGER.info("{}loading {}, {} connections, {} servers.", new Object[]{id.tag, id.uri, Integer.valueOf(loaded), Integer.valueOf(servers.size())});
                    }
                } else {
                    SerializationUtil.skipItems(in, 16);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("loading failed:", e);
        } catch (IllegalArgumentException e2) {
            LOGGER.warn("loading failed:", e2);
        }
        LOGGER.info("load: {} ms, {} connections", Long.valueOf(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time)), Integer.valueOf(count));
        return count;
    }

    public static int saveServers(OutputStream out, long maxQuietPeriodInSeconds, CoapServer... servers) throws IOException {
        return saveServers(out, maxQuietPeriodInSeconds, (List<CoapServer>) Arrays.asList(servers));
    }

    public static int saveServers(OutputStream out, long maxQuietPeriodInSeconds, List<CoapServer> servers) throws IOException {
        int count = 0;
        for (CoapServer server : servers) {
            server.stop();
        }
        long start = System.nanoTime();
        DatagramWriter writer = new DatagramWriter();
        SerializationUtil.writeNanotimeSynchronizationMark(writer);
        writer.writeTo(out);
        for (CoapServer server2 : servers) {
            count += server2.saveAllConnectors(out, maxQuietPeriodInSeconds);
        }
        SerializationUtil.write(writer, (String) null, 8);
        writer.writeTo(out);
        long time = System.nanoTime() - start;
        LOGGER.info("save: {} ms, {} connections", Long.valueOf(TimeUnit.NANOSECONDS.toMillis(time)), Integer.valueOf(count));
        return count;
    }
}
