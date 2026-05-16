package org.eclipse.californium.core;

import java.util.List;
import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.californium.core.server.resources.ResourceAttributes;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/WebLink.class */
public class WebLink implements Comparable<WebLink> {
    private String uri;
    private final ResourceAttributes attributes = new ResourceAttributes();

    public WebLink(String uri) {
        this.uri = uri;
    }

    public String getURI() {
        return this.uri;
    }

    public ResourceAttributes getAttributes() {
        return this.attributes;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        builder.append(this.uri);
        builder.append('>');
        if (this.attributes.containsAttribute(LinkFormat.TITLE)) {
            builder.append(' ').append(this.attributes.getTitle());
        }
        append(builder, LinkFormat.RESOURCE_TYPE);
        append(builder, LinkFormat.INTERFACE_DESCRIPTION);
        append(builder, LinkFormat.CONTENT_TYPE);
        append(builder, LinkFormat.MAX_SIZE_ESTIMATE);
        append(builder, LinkFormat.OBSERVABLE);
        return builder.toString();
    }

    private void append(StringBuilder builder, String attributeName) {
        if (this.attributes.containsAttribute(attributeName)) {
            builder.append(StringUtil.lineSeparator()).append("\t").append(attributeName);
            List<String> values = this.attributes.getAttributeValues(attributeName);
            if (!values.isEmpty()) {
                builder.append(":\t").append(values);
            }
        }
    }

    @Override // java.lang.Comparable
    public int compareTo(WebLink other) {
        return this.uri.compareTo(other.getURI());
    }
}
