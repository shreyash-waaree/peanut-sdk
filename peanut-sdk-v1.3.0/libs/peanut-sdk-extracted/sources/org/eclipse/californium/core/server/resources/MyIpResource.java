package org.eclipse.californium.core.server.resources;

import java.net.InetSocketAddress;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/resources/MyIpResource.class */
public class MyIpResource extends CoapResource {
    public static final String RESOURCE_NAME = "myip";

    public MyIpResource(String name, boolean visible) {
        super(name, visible);
        getAttributes().setTitle("MyIP");
        getAttributes().addContentType(0);
        getAttributes().addContentType(50);
        getAttributes().addContentType(41);
    }

    @Override // org.eclipse.californium.core.CoapResource
    public void handleGET(CoapExchange exchange) {
        byte[] payload;
        Request request = exchange.advanced().getRequest();
        int accept = request.getOptions().getAccept();
        if (accept == -1) {
            accept = 0;
        }
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.getOptions().setContentFormat(accept);
        switch (accept) {
            case 0:
                payload = handleGetFormat(exchange, "%1$s");
                break;
            case 41:
                payload = handleGetFormat(exchange, "<ip host=\"%2$s\" port=\"%3$d\" />");
                break;
            case 50:
                payload = handleGetFormat(exchange, "{ \"ip\" : \"%2$s\",\n \"port\" : %3$d }");
                break;
            default:
                String ct = MediaTypeRegistry.toString(accept);
                exchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Type \"" + ct + "\" is not supported for this resource!", 0);
                return;
        }
        response.setPayload(payload);
        exchange.respond(response);
    }

    private byte[] handleGetFormat(CoapExchange exchange, String format) {
        InetSocketAddress source = exchange.advanced().getRequest().getSourceContext().getPeerAddress();
        String address = StringUtil.toString(source);
        String host = StringUtil.toString(source.getAddress());
        return String.format(format, address, host, Integer.valueOf(source.getPort())).getBytes();
    }
}
