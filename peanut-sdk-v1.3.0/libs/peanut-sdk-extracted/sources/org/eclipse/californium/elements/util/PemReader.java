package org.eclipse.californium.elements.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/PemReader.class */
public class PemReader {
    public static final Logger LOGGER = LoggerFactory.getLogger(PemReader.class);
    private static final Pattern BEGIN_PATTERN = Pattern.compile("^\\-+BEGIN\\s+([\\w\\s]+)\\-+$");
    private static final Pattern END_PATTERN = Pattern.compile("^\\-+END\\s+([\\w\\s]+)\\-+$");
    private BufferedReader reader;
    private String tag;

    public PemReader(InputStream in) {
        this.reader = new BufferedReader(new InputStreamReader(in));
    }

    public void close() {
        try {
            this.reader.close();
        } catch (IOException e) {
        }
    }

    public String readNextBegin() throws IOException {
        this.tag = null;
        while (true) {
            String line = this.reader.readLine();
            if (line == null) {
                break;
            }
            Matcher matcher = BEGIN_PATTERN.matcher(line);
            if (matcher.matches()) {
                this.tag = matcher.group(1);
                LOGGER.debug("Found Begin of {}", this.tag);
                break;
            }
        }
        return this.tag;
    }

    public byte[] readToEnd() throws IOException {
        StringBuilder buffer = new StringBuilder();
        while (true) {
            String line = this.reader.readLine();
            if (line == null) {
                break;
            }
            Matcher matcher = END_PATTERN.matcher(line);
            if (matcher.matches()) {
                String end = matcher.group(1);
                if (end.equals(this.tag)) {
                    byte[] decode = Base64.decode(buffer.toString());
                    LOGGER.debug("Found End of {}", this.tag);
                    return decode;
                }
                LOGGER.warn("Found End of {}, but expected {}!", end, this.tag);
            } else {
                buffer.append(line);
            }
        }
        this.tag = null;
        return null;
    }
}
