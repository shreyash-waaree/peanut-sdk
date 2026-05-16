package org.eclipse.californium.core.coap;

import com.keenon.sdk.constant.ApiConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceAttributes;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/LinkFormat.class */
public class LinkFormat {
    public static final String RESOURCE_TYPE = "rt";
    public static final String INTERFACE_DESCRIPTION = "if";
    public static final String CONTENT_TYPE = "ct";
    public static final String MAX_SIZE_ESTIMATE = "sz";
    public static final String TITLE = "title";
    public static final String OBSERVABLE = "obs";
    public static final String LINK = "href";
    public static final String LIFE_TIME = "lt";
    public static final String SECTOR = "d";
    public static final String CONTEXT = "anchor";
    public static final String BASE = "base";
    public static final String RELATION = "rel";
    public static final String END_POINT = "ep";
    public static final String END_POINT_TYPE = "et";
    public static final String COUNT = "count";
    public static final String PAGE = "page";
    public static final Pattern DELIMITER = Pattern.compile("\\s*,+\\s*");
    public static final Pattern SEPARATOR = Pattern.compile("\\s*;+\\s*");
    public static final Pattern WORD = Pattern.compile("\\w+");
    public static final Pattern QUOTED_STRING = Pattern.compile("\\G\".*?\"");
    public static final Pattern CARDINAL = Pattern.compile("\\G\\d+");

    public static String serializeTree(Resource resource) {
        StringBuilder buffer = new StringBuilder();
        List<String> noQueries = Collections.emptyList();
        for (Resource child : resource.getChildren()) {
            serializeTree(child, noQueries, buffer);
        }
        if (buffer.length() > 1) {
            buffer.delete(buffer.length() - 1, buffer.length());
        }
        return buffer.toString();
    }

    public static void serializeTree(Resource resource, List<String> queries, StringBuilder buffer) {
        if (resource.isVisible() && matches(resource, queries)) {
            buffer.append((CharSequence) serializeResource(resource));
        }
        List<Resource> childs = new ArrayList<>(resource.getChildren());
        Collections.sort(childs, new Comparator<Resource>() { // from class: org.eclipse.californium.core.coap.LinkFormat.1
            @Override // java.util.Comparator
            public int compare(Resource o1, Resource o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (Resource child : childs) {
            serializeTree(child, queries, buffer);
        }
    }

    public static StringBuilder serializeResource(Resource resource) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<").append(resource.getPath()).append(resource.getName()).append(">").append((CharSequence) serializeAttributes(resource.getAttributes())).append(ApiConstants.DELIMITER_COMMA);
        return buffer;
    }

    public static StringBuilder serializeAttributes(ResourceAttributes attributes) {
        StringBuilder buffer = new StringBuilder();
        List<String> attributesList = new ArrayList<>(attributes.getAttributeKeySet());
        Collections.sort(attributesList);
        for (String attr : attributesList) {
            List<String> values = attributes.getAttributeValues(attr);
            if (values != null) {
                buffer.append(";");
                buffer.append((CharSequence) serializeAttribute(attr, new LinkedList(values)));
            }
        }
        return buffer;
    }

    public static StringBuilder serializeAttribute(String key, List<String> values) {
        StringBuilder linkFormat = new StringBuilder();
        boolean quotes = false;
        linkFormat.append(key);
        if (values == null) {
            throw new RuntimeException("Values null");
        }
        if (values.isEmpty() || values.get(0).equals("")) {
            return linkFormat;
        }
        linkFormat.append("=");
        if (values.size() > 1 || !values.get(0).matches("^[0-9]+$")) {
            linkFormat.append('\"');
            quotes = true;
        }
        Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            linkFormat.append(it.next());
            if (it.hasNext()) {
                linkFormat.append(' ');
            }
        }
        if (quotes) {
            linkFormat.append('\"');
        }
        return linkFormat;
    }

    public static boolean matches(Resource resource, List<String> queries) {
        if (resource == null) {
            return false;
        }
        if (queries == null || queries.size() == 0) {
            return true;
        }
        ResourceAttributes attributes = resource.getAttributes();
        String path = resource.getPath() + resource.getName();
        for (String s : queries) {
            int delim = s.indexOf("=");
            if (delim != -1) {
                String attrName = s.substring(0, delim);
                String expected = s.substring(delim + 1);
                if (attrName.equals(LINK)) {
                    if (expected.endsWith("*")) {
                        if (!path.startsWith(expected.substring(0, expected.length() - 1))) {
                            return false;
                        }
                    } else if (!path.equals(expected)) {
                        return false;
                    }
                } else if (attributes.containsAttribute(attrName)) {
                    boolean matched = false;
                    Iterator<String> it = attributes.getAttributeValues(attrName).iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        String actual = it.next();
                        int prefixLength = expected.indexOf(42);
                        if (prefixLength < 0 || prefixLength >= actual.length()) {
                            if (actual.equals(expected)) {
                                matched = true;
                                break;
                            }
                        } else {
                            String shortened = expected.substring(0, prefixLength);
                            if (actual.substring(0, prefixLength).equals(shortened)) {
                                matched = true;
                                break;
                            }
                        }
                    }
                    if (!matched) {
                        return false;
                    }
                } else if (!attributes.containsAttribute(attrName)) {
                    return false;
                }
            } else if (attributes.getAttributeValues(s).size() == 0) {
                return false;
            }
        }
        return true;
    }

    public static Set<WebLink> parse(String linkFormat) {
        String attr;
        Pattern DELIMITER2 = Pattern.compile("\\s*,+\\s*");
        Set<WebLink> links = new ConcurrentSkipListSet<>();
        if (linkFormat != null) {
            Scanner scanner = new Scanner(linkFormat);
            while (true) {
                String path = scanner.findInLine("<[^>]*>");
                if (path == null) {
                    break;
                }
                WebLink link = new WebLink(path.substring(1, path.length() - 1));
                while (scanner.findWithinHorizon(DELIMITER2, 1) == null && (attr = scanner.findInLine(WORD)) != null) {
                    if (scanner.findWithinHorizon("=", 1) != null) {
                        String value = scanner.findInLine(QUOTED_STRING);
                        if (value != null) {
                            String value2 = value.substring(1, value.length() - 1);
                            if (attr.equals(TITLE)) {
                                link.getAttributes().addAttribute(attr, value2);
                            } else {
                                for (String part : value2.split("\\s", 0)) {
                                    link.getAttributes().addAttribute(attr, part);
                                }
                            }
                        } else {
                            String value3 = scanner.findInLine(WORD);
                            if (value3 != null) {
                                link.getAttributes().setAttribute(attr, value3);
                            } else {
                                String value4 = scanner.findInLine(CARDINAL);
                                if (value4 != null) {
                                    link.getAttributes().setAttribute(attr, value4);
                                } else if (scanner.hasNext()) {
                                    scanner.next();
                                }
                            }
                        }
                    } else {
                        link.getAttributes().addAttribute(attr);
                    }
                }
                links.add(link);
            }
            scanner.close();
        }
        return links;
    }
}
