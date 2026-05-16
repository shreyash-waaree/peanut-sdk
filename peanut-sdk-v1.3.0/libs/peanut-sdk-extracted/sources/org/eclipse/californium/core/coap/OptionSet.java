package org.eclipse.californium.core.coap;

import com.keenon.sdk.constant.ApiConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.californium.elements.util.Bytes;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/OptionSet.class */
public final class OptionSet {
    private static final int MAX_OBSERVE_NO = 16777215;
    private List<byte[]> if_match_list;
    private String uri_host;
    private List<byte[]> etag_list;
    private boolean if_none_match;
    private Integer uri_port;
    private List<String> location_path_list;
    private List<String> uri_path_list;
    private Integer content_format;
    private Long max_age;
    private List<String> uri_query_list;
    private Integer accept;
    private List<String> location_query_list;
    private String proxy_uri;
    private String proxy_scheme;
    private BlockOption block1;
    private BlockOption block2;
    private Integer size1;
    private Integer size2;
    private Integer observe;
    private byte[] oscore;
    private NoResponseOption no_response;
    private List<Option> others;
    private boolean explicitUriOptions;

    public OptionSet() {
        this.if_match_list = null;
        this.uri_host = null;
        this.etag_list = null;
        this.if_none_match = false;
        this.uri_port = null;
        this.location_path_list = null;
        this.uri_path_list = null;
        this.content_format = null;
        this.max_age = null;
        this.uri_query_list = null;
        this.accept = null;
        this.location_query_list = null;
        this.proxy_uri = null;
        this.proxy_scheme = null;
        this.block1 = null;
        this.block2 = null;
        this.size1 = null;
        this.size2 = null;
        this.observe = null;
        this.oscore = null;
        this.no_response = null;
        this.others = null;
    }

    public OptionSet(OptionSet origin) {
        if (origin == null) {
            throw new NullPointerException("option set must not be null!");
        }
        this.if_match_list = copyList(origin.if_match_list);
        this.uri_host = origin.uri_host;
        this.etag_list = copyList(origin.etag_list);
        this.if_none_match = origin.if_none_match;
        this.uri_port = origin.uri_port;
        this.location_path_list = copyList(origin.location_path_list);
        this.uri_path_list = copyList(origin.uri_path_list);
        this.content_format = origin.content_format;
        this.max_age = origin.max_age;
        this.uri_query_list = copyList(origin.uri_query_list);
        this.accept = origin.accept;
        this.location_query_list = copyList(origin.location_query_list);
        this.proxy_uri = origin.proxy_uri;
        this.proxy_scheme = origin.proxy_scheme;
        this.block1 = origin.block1;
        this.block2 = origin.block2;
        this.size1 = origin.size1;
        this.size2 = origin.size2;
        this.observe = origin.observe;
        if (origin.oscore != null) {
            this.oscore = (byte[]) origin.oscore.clone();
        }
        this.no_response = origin.no_response;
        this.others = copyList(origin.others);
    }

    public void clear() {
        if (this.if_match_list != null) {
            this.if_match_list.clear();
        }
        this.uri_host = null;
        if (this.etag_list != null) {
            this.etag_list.clear();
        }
        this.if_none_match = false;
        this.uri_port = null;
        if (this.location_path_list != null) {
            this.location_path_list.clear();
        }
        if (this.uri_path_list != null) {
            this.uri_path_list.clear();
        }
        this.content_format = null;
        this.max_age = null;
        if (this.uri_query_list != null) {
            this.uri_query_list.clear();
        }
        this.accept = null;
        if (this.location_query_list != null) {
            this.location_query_list.clear();
        }
        this.proxy_uri = null;
        this.proxy_scheme = null;
        this.block1 = null;
        this.block2 = null;
        this.size1 = null;
        this.size2 = null;
        this.observe = null;
        this.oscore = null;
        this.no_response = null;
        if (this.others != null) {
            this.others.clear();
        }
    }

    private <T> List<T> copyList(List<T> list) {
        if (list == null) {
            return null;
        }
        return new LinkedList(list);
    }

    public List<byte[]> getIfMatch() {
        synchronized (this) {
            if (this.if_match_list == null) {
                this.if_match_list = new LinkedList();
            }
        }
        return this.if_match_list;
    }

    public int getIfMatchCount() {
        return getIfMatch().size();
    }

    public boolean isIfMatch(byte[] check) {
        if (this.if_match_list == null) {
            return true;
        }
        for (byte[] etag : this.if_match_list) {
            if (etag.length == 0 || Arrays.equals(etag, check)) {
                return true;
            }
        }
        return false;
    }

    public OptionSet addIfMatch(byte[] etag) {
        checkOptionValue(1, etag);
        getIfMatch().add(etag);
        return this;
    }

    public OptionSet removeIfMatch(byte[] etag) {
        getIfMatch().remove(etag);
        return this;
    }

    public OptionSet clearIfMatchs() {
        getIfMatch().clear();
        return this;
    }

    public String getUriHost() {
        return this.uri_host;
    }

    public boolean hasUriHost() {
        return this.uri_host != null;
    }

    public OptionSet setUriHost(String host) {
        checkOptionValue(3, host);
        this.uri_host = host;
        return this;
    }

    public OptionSet removeUriHost() {
        this.uri_host = null;
        return this;
    }

    public List<byte[]> getETags() {
        synchronized (this) {
            if (this.etag_list == null) {
                this.etag_list = new LinkedList();
            }
        }
        return this.etag_list;
    }

    public int getETagCount() {
        return getETags().size();
    }

    public boolean containsETag(byte[] check) {
        if (this.etag_list == null) {
            return false;
        }
        for (byte[] etag : this.etag_list) {
            if (Arrays.equals(etag, check)) {
                return true;
            }
        }
        return false;
    }

    public OptionSet addETag(byte[] etag) {
        checkOptionValue(4, etag);
        if (!containsETag(etag)) {
            getETags().add((byte[]) etag.clone());
        }
        return this;
    }

    public OptionSet removeETag(byte[] etag) {
        checkOptionValue(4, etag);
        if (this.etag_list != null) {
            int index = 0;
            while (true) {
                if (index >= this.etag_list.size()) {
                    break;
                }
                if (!Arrays.equals(this.etag_list.get(index), etag)) {
                    index++;
                } else {
                    this.etag_list.remove(index);
                    break;
                }
            }
        }
        return this;
    }

    public OptionSet clearETags() {
        getETags().clear();
        return this;
    }

    public boolean hasIfNoneMatch() {
        return this.if_none_match;
    }

    public OptionSet setIfNoneMatch(boolean present) {
        this.if_none_match = present;
        return this;
    }

    public Integer getUriPort() {
        return this.uri_port;
    }

    public boolean hasUriPort() {
        return this.uri_port != null;
    }

    public OptionSet setUriPort(int port) {
        OptionNumberRegistry.assertValue(7, port);
        this.uri_port = Integer.valueOf(port);
        return this;
    }

    public OptionSet removeUriPort() {
        this.uri_port = null;
        return this;
    }

    public List<String> getLocationPath() {
        synchronized (this) {
            if (this.location_path_list == null) {
                this.location_path_list = new LinkedList();
            }
        }
        return this.location_path_list;
    }

    public String getLocationString() {
        StringBuilder builder = new StringBuilder();
        builder.append('/');
        appendMultiOption(builder, getLocationPath(), '/');
        if (getLocationQueryCount() > 0) {
            builder.append('?');
            appendMultiOption(builder, getLocationQuery(), '&');
        }
        return builder.toString();
    }

    public String getLocationPathString() {
        return getMultiOptionString(getLocationPath(), '/');
    }

    public int getLocationPathCount() {
        return getLocationPath().size();
    }

    public OptionSet addLocationPath(String segment) {
        checkOptionValue(8, segment);
        getLocationPath().add(segment);
        return this;
    }

    public OptionSet clearLocationPath() {
        getLocationPath().clear();
        return this;
    }

    public OptionSet setLocationPath(String path) {
        if (path.startsWith("/")) {
            path = path.substring("/".length());
        }
        clearLocationPath();
        for (String segment : path.split("/")) {
            addLocationPath(segment);
        }
        return this;
    }

    public String getUriString() {
        StringBuilder builder = new StringBuilder();
        builder.append('/');
        appendMultiOption(builder, getUriPath(), '/');
        if (getURIQueryCount() > 0) {
            builder.append('?');
            appendMultiOption(builder, getUriQuery(), '&');
        }
        return builder.toString();
    }

    public List<String> getUriPath() {
        synchronized (this) {
            if (this.uri_path_list == null) {
                this.uri_path_list = new LinkedList();
            }
        }
        return this.uri_path_list;
    }

    public String getUriPathString() {
        return getMultiOptionString(getUriPath(), '/');
    }

    public int getURIPathCount() {
        return getUriPath().size();
    }

    public OptionSet setUriPath(String path) {
        if (path.startsWith("/")) {
            path = path.substring("/".length());
        }
        clearUriPath();
        for (String segment : path.split("/")) {
            addUriPath(segment);
        }
        return this;
    }

    public OptionSet addUriPath(String segment) {
        checkOptionValue(11, segment);
        getUriPath().add(segment);
        this.explicitUriOptions = true;
        return this;
    }

    public OptionSet clearUriPath() {
        getUriPath().clear();
        return this;
    }

    public int getContentFormat() {
        if (hasContentFormat()) {
            return this.content_format.intValue();
        }
        return -1;
    }

    public boolean hasContentFormat() {
        return this.content_format != null;
    }

    public boolean isContentFormat(int format) {
        return this.content_format != null && this.content_format.intValue() == format;
    }

    public OptionSet setContentFormat(int format) {
        if (-1 == format) {
            this.content_format = null;
        } else {
            OptionNumberRegistry.assertValue(12, format);
            this.content_format = Integer.valueOf(format);
        }
        return this;
    }

    public OptionSet removeContentFormat() {
        this.content_format = null;
        return this;
    }

    public Long getMaxAge() {
        Long m = this.max_age;
        return Long.valueOf(m != null ? m.longValue() : 60L);
    }

    public boolean hasMaxAge() {
        return this.max_age != null;
    }

    public OptionSet setMaxAge(long age) {
        OptionNumberRegistry.assertValue(14, age);
        this.max_age = Long.valueOf(age);
        return this;
    }

    public OptionSet removeMaxAge() {
        this.max_age = null;
        return this;
    }

    public List<String> getUriQuery() {
        synchronized (this) {
            if (this.uri_query_list == null) {
                this.uri_query_list = new LinkedList();
            }
        }
        return this.uri_query_list;
    }

    public int getURIQueryCount() {
        return getUriQuery().size();
    }

    public String getUriQueryString() {
        return getMultiOptionString(getUriQuery(), '&');
    }

    public OptionSet setUriQuery(String query) {
        while (query.startsWith("?")) {
            query = query.substring(1);
        }
        clearUriQuery();
        for (String segment : query.split("&")) {
            if (!segment.isEmpty()) {
                addUriQuery(segment);
            }
        }
        return this;
    }

    public OptionSet addUriQuery(String argument) {
        checkOptionValue(15, argument);
        getUriQuery().add(argument);
        this.explicitUriOptions = true;
        return this;
    }

    public OptionSet removeUriQuery(String argument) {
        getUriQuery().remove(argument);
        return this;
    }

    public OptionSet clearUriQuery() {
        getUriQuery().clear();
        return this;
    }

    public int getAccept() {
        if (hasAccept()) {
            return this.accept.intValue();
        }
        return -1;
    }

    public boolean hasAccept() {
        return this.accept != null;
    }

    public boolean isAccept(int format) {
        return this.accept != null && this.accept.intValue() == format;
    }

    public OptionSet setAccept(int format) {
        OptionNumberRegistry.assertValue(17, format);
        this.accept = Integer.valueOf(format);
        return this;
    }

    public OptionSet removeAccept() {
        this.accept = null;
        return this;
    }

    public List<String> getLocationQuery() {
        synchronized (this) {
            if (this.location_query_list == null) {
                this.location_query_list = new LinkedList();
            }
        }
        return this.location_query_list;
    }

    public int getLocationQueryCount() {
        return getLocationQuery().size();
    }

    public String getLocationQueryString() {
        return getMultiOptionString(getLocationQuery(), '&');
    }

    public OptionSet setLocationQuery(String query) {
        while (query.startsWith("?")) {
            query = query.substring(1);
        }
        clearLocationQuery();
        for (String segment : query.split("&")) {
            if (!segment.isEmpty()) {
                addLocationQuery(segment);
            }
        }
        return this;
    }

    public OptionSet addLocationQuery(String argument) {
        checkOptionValue(20, argument);
        getLocationQuery().add(argument);
        return this;
    }

    public OptionSet removeLocationQuery(String argument) {
        getLocationQuery().remove(argument);
        return this;
    }

    public OptionSet clearLocationQuery() {
        getLocationQuery().clear();
        return this;
    }

    public String getProxyUri() {
        return this.proxy_uri;
    }

    public boolean hasProxyUri() {
        return this.proxy_uri != null;
    }

    public OptionSet setProxyUri(String uri) {
        checkOptionValue(35, uri);
        this.proxy_uri = uri;
        return this;
    }

    public OptionSet removeProxyUri() {
        this.proxy_uri = null;
        return this;
    }

    public String getProxyScheme() {
        return this.proxy_scheme;
    }

    public boolean hasProxyScheme() {
        return this.proxy_scheme != null;
    }

    public OptionSet setProxyScheme(String scheme) {
        checkOptionValue(39, scheme);
        this.proxy_scheme = scheme;
        return this;
    }

    public OptionSet removeProxyScheme() {
        this.proxy_scheme = null;
        return this;
    }

    public BlockOption getBlock1() {
        return this.block1;
    }

    public boolean hasBlock1() {
        return this.block1 != null;
    }

    public OptionSet setBlock1(int szx, boolean m, int num) {
        this.block1 = new BlockOption(szx, m, num);
        return this;
    }

    public OptionSet setBlock1(byte[] value) {
        this.block1 = new BlockOption(value);
        return this;
    }

    public OptionSet setBlock1(BlockOption block) {
        this.block1 = block;
        return this;
    }

    public OptionSet removeBlock1() {
        this.block1 = null;
        return this;
    }

    public BlockOption getBlock2() {
        return this.block2;
    }

    public boolean hasBlock2() {
        return this.block2 != null;
    }

    public OptionSet setBlock2(int szx, boolean m, int num) {
        this.block2 = new BlockOption(szx, m, num);
        return this;
    }

    public OptionSet setBlock2(byte[] value) {
        this.block2 = new BlockOption(value);
        return this;
    }

    public OptionSet setBlock2(BlockOption block) {
        this.block2 = block;
        return this;
    }

    public OptionSet removeBlock2() {
        this.block2 = null;
        return this;
    }

    public Integer getSize1() {
        return this.size1;
    }

    public boolean hasSize1() {
        return this.size1 != null;
    }

    public OptionSet setSize1(int size) {
        this.size1 = Integer.valueOf(size);
        return this;
    }

    public OptionSet removeSize1() {
        this.size1 = null;
        return this;
    }

    public Integer getSize2() {
        return this.size2;
    }

    public boolean hasSize2() {
        return this.size2 != null;
    }

    public OptionSet setSize2(int size) {
        this.size2 = Integer.valueOf(size);
        return this;
    }

    public OptionSet removeSize2() {
        this.size2 = null;
        return this;
    }

    public Integer getObserve() {
        return this.observe;
    }

    public boolean hasObserve() {
        return this.observe != null;
    }

    public OptionSet setObserve(int seqnum) {
        OptionNumberRegistry.assertValue(6, seqnum);
        this.observe = Integer.valueOf(seqnum);
        return this;
    }

    public OptionSet removeObserve() {
        this.observe = null;
        return this;
    }

    public static boolean isValidObserveOption(int value) {
        return value >= 0 && value <= MAX_OBSERVE_NO;
    }

    public byte[] getOscore() {
        return this.oscore;
    }

    public boolean hasOscore() {
        return this.oscore != null;
    }

    public OptionSet setOscore(byte[] oscore) {
        checkOptionValue(9, oscore);
        this.oscore = (byte[]) oscore.clone();
        return this;
    }

    public OptionSet removeOscore() {
        this.oscore = null;
        return this;
    }

    public NoResponseOption getNoResponse() {
        return this.no_response;
    }

    public boolean hasNoResponse() {
        return this.no_response != null;
    }

    public OptionSet setNoResponse(int noResponse) {
        this.no_response = new NoResponseOption(noResponse);
        return this;
    }

    public OptionSet setNoResponse(NoResponseOption noResponse) {
        this.no_response = noResponse;
        return this;
    }

    public OptionSet removeNoResponse() {
        this.no_response = null;
        return this;
    }

    public boolean hasOption(int number) {
        return Collections.binarySearch(asSortedList(), new Option(number)) >= 0;
    }

    private List<Option> getOthersInternal() {
        synchronized (this) {
            if (this.others == null) {
                this.others = new LinkedList();
            }
        }
        return this.others;
    }

    public List<Option> getOthers() {
        List<Option> others = this.others;
        if (others == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(others);
    }

    public List<Option> asSortedList() {
        ArrayList<Option> options = new ArrayList<>();
        if (this.if_match_list != null) {
            for (byte[] value : this.if_match_list) {
                options.add(new Option(1, value));
            }
        }
        if (hasUriHost()) {
            options.add(new Option(3, getUriHost()));
        }
        if (this.etag_list != null) {
            for (byte[] value2 : this.etag_list) {
                options.add(new Option(4, value2));
            }
        }
        if (hasIfNoneMatch()) {
            options.add(new Option(5, Bytes.EMPTY));
        }
        if (hasUriPort()) {
            options.add(new Option(7, getUriPort().intValue()));
        }
        if (this.location_path_list != null) {
            for (String str : this.location_path_list) {
                options.add(new Option(8, str));
            }
        }
        if (this.uri_path_list != null) {
            for (String str2 : this.uri_path_list) {
                options.add(new Option(11, str2));
            }
        }
        if (hasContentFormat()) {
            options.add(new Option(12, getContentFormat()));
        }
        if (hasMaxAge()) {
            options.add(new Option(14, getMaxAge().longValue()));
        }
        if (this.uri_query_list != null) {
            for (String str3 : this.uri_query_list) {
                options.add(new Option(15, str3));
            }
        }
        if (hasAccept()) {
            options.add(new Option(17, getAccept()));
        }
        if (this.location_query_list != null) {
            for (String str4 : this.location_query_list) {
                options.add(new Option(20, str4));
            }
        }
        if (hasProxyUri()) {
            options.add(new Option(35, getProxyUri()));
        }
        if (hasProxyScheme()) {
            options.add(new Option(39, getProxyScheme()));
        }
        if (hasObserve()) {
            options.add(new Option(6, getObserve().intValue()));
        }
        if (hasBlock1()) {
            options.add(new Option(27, getBlock1().getValue()));
        }
        if (hasBlock2()) {
            options.add(new Option(23, getBlock2().getValue()));
        }
        if (hasSize1()) {
            options.add(new Option(60, getSize1().intValue()));
        }
        if (hasSize2()) {
            options.add(new Option(28, getSize2().intValue()));
        }
        if (hasOscore()) {
            options.add(new Option(9, getOscore()));
        }
        if (hasNoResponse()) {
            options.add(getNoResponse().toOption());
        }
        if (this.others != null) {
            options.addAll(this.others);
        }
        Collections.sort(options);
        return options;
    }

    boolean hasExplicitUriOptions() {
        return this.explicitUriOptions;
    }

    void resetExplicitUriOptions() {
        this.explicitUriOptions = false;
    }

    public OptionSet addOptions(Option... options) {
        if (options != null) {
            for (Option option : options) {
                addOption(option);
            }
        }
        return this;
    }

    public OptionSet addOptions(List<Option> options) {
        if (options != null) {
            for (Option option : options) {
                addOption(option);
            }
        }
        return this;
    }

    public OptionSet addOption(Option option) {
        switch (option.getNumber()) {
            case 1:
                addIfMatch(option.getValue());
                break;
            case 3:
                setUriHost(option.getStringValue());
                break;
            case 4:
                addETag(option.getValue());
                break;
            case 5:
                setIfNoneMatch(true);
                break;
            case 6:
                setObserve(option.getIntegerValue());
                break;
            case 7:
                setUriPort(option.getIntegerValue());
                break;
            case 8:
                addLocationPath(option.getStringValue());
                break;
            case 9:
                setOscore(option.getValue());
                break;
            case 11:
                addUriPath(option.getStringValue());
                break;
            case 12:
                setContentFormat(option.getIntegerValue());
                break;
            case 14:
                setMaxAge(option.getLongValue());
                break;
            case 15:
                addUriQuery(option.getStringValue());
                break;
            case 17:
                setAccept(option.getIntegerValue());
                break;
            case 20:
                addLocationQuery(option.getStringValue());
                break;
            case 23:
                setBlock2(option.getValue());
                break;
            case OptionNumberRegistry.BLOCK1 /* 27 */:
                setBlock1(option.getValue());
                break;
            case OptionNumberRegistry.SIZE2 /* 28 */:
                setSize2(option.getIntegerValue());
                break;
            case 35:
                setProxyUri(option.getStringValue());
                break;
            case 39:
                setProxyScheme(option.getStringValue());
                break;
            case 60:
                setSize1(option.getIntegerValue());
                break;
            case OptionNumberRegistry.NO_RESPONSE /* 258 */:
                setNoResponse(option.getIntegerValue());
                break;
            default:
                getOthersInternal().add(option);
                break;
        }
        return this;
    }

    public OptionSet addOtherOption(Option option) {
        getOthersInternal().add(option);
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        StringBuilder sbv = new StringBuilder();
        int oldNr = -1;
        boolean list = false;
        sb.append('{');
        for (Option opt : asSortedList()) {
            if (opt.getNumber() != oldNr) {
                if (oldNr != -1) {
                    if (list) {
                        sbv.append(']');
                    }
                    sb.append(sbv.toString()).append(", ");
                    sbv.setLength(0);
                }
                list = false;
                sb.append('\"');
                sb.append(OptionNumberRegistry.toString(opt.getNumber()));
                sb.append('\"');
                sb.append(':');
            } else {
                if (!list) {
                    sbv.insert(0, '[');
                }
                list = true;
                sbv.append(ApiConstants.DELIMITER_COMMA);
            }
            sbv.append(opt.toValueString());
            oldNr = opt.getNumber();
        }
        if (list) {
            sbv.append(']');
        }
        sb.append(sbv.toString());
        sb.append('}');
        return sb.toString();
    }

    private String getMultiOptionString(List<String> multiOption, char separator) {
        StringBuilder builder = new StringBuilder();
        appendMultiOption(builder, multiOption, separator);
        return builder.toString();
    }

    private void appendMultiOption(StringBuilder builder, List<String> multiOption, char separator) {
        if (!multiOption.isEmpty()) {
            for (String optionText : multiOption) {
                builder.append(optionText).append(separator);
            }
            builder.setLength(builder.length() - 1);
        }
    }

    private static void checkOptionValue(int optionNumber, String value) {
        checkOptionValue(optionNumber, value == null ? null : value.getBytes(CoAP.UTF8_CHARSET));
    }

    private static void checkOptionValue(int optionNumber, byte[] value) {
        if (value == null) {
            String optionName = OptionNumberRegistry.toString(optionNumber);
            throw new NullPointerException(optionName + " option must not be null!");
        }
        OptionNumberRegistry.assertValueLength(optionNumber, value.length);
    }
}
