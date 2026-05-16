package org.eclipse.californium.elements.config;

import com.keenon.sdk.constant.ApiConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.elements.Definitions;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/Configuration.class */
public final class Configuration {
    public static final String DEFAULT_HEADER = "Californium3 CoAP Properties file";
    private static Configuration standard;
    private final ConcurrentMap<String, DefinitionsProvider> modules;
    private final Definitions<DocumentedDefinition<?>> definitions;
    private final Map<String, Object> values;
    private final Set<String> transientValues;
    public static final String DEFAULT_FILE_NAME = "Californium3.properties";
    public static final File DEFAULT_FILE = new File(DEFAULT_FILE_NAME);
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
    private static final ConcurrentMap<String, DefinitionsProvider> DEFAULT_MODULES = new ConcurrentHashMap();
    private static final Definitions<DocumentedDefinition<?>> DEFAULT_DEFINITIONS = new Definitions<>("Configuration");

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/Configuration$DefinitionsProvider.class */
    public interface DefinitionsProvider {
        void applyDefinitions(Configuration configuration);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/Configuration$ModuleDefinitionsProvider.class */
    public interface ModuleDefinitionsProvider extends DefinitionsProvider {
        String getModule();
    }

    private static boolean addModule(ConcurrentMap<String, DefinitionsProvider> modules, ModuleDefinitionsProvider definitionsProvider) {
        if (modules == null) {
            throw new NullPointerException("Modules must not be null!");
        }
        if (definitionsProvider == null) {
            throw new NullPointerException("DefinitionsProvider must not be null!");
        }
        String module = definitionsProvider.getModule();
        if (module == null) {
            throw new IllegalArgumentException("DefinitionsProvider's module must not be null!");
        }
        if (module.isEmpty()) {
            throw new IllegalArgumentException("DefinitionsProvider's module name must not be empty!");
        }
        DefinitionsProvider previous = modules.putIfAbsent(module, definitionsProvider);
        if (previous == null || previous == definitionsProvider) {
            return previous == null;
        }
        throw new IllegalArgumentException("Module " + module + " already registered with different provider!");
    }

    public static void addDefaultModule(ModuleDefinitionsProvider definitionsProvider) {
        if (addModule(DEFAULT_MODULES, definitionsProvider)) {
            LOGGER.info("defaults added {}", definitionsProvider.getModule());
        }
    }

    public static Configuration getStandard() {
        synchronized (Configuration.class) {
            if (standard == null) {
                createStandardWithFile(DEFAULT_FILE);
            }
        }
        return standard;
    }

    public static void setStandard(Configuration standard2) {
        standard = standard2;
    }

    public static Configuration createStandardWithoutFile() {
        LOGGER.info("Creating standard configuration properties without a file");
        standard = new Configuration();
        return standard;
    }

    public static Configuration createStandardFromStream(InputStream inStream) {
        standard = createFromStream(inStream, null);
        return standard;
    }

    public static Configuration createFromStream(InputStream inStream, DefinitionsProvider customProvider) {
        LOGGER.info("Creating configuration properties from stream");
        Configuration configuration = new Configuration();
        configuration.apply(customProvider);
        try {
            configuration.load(inStream);
        } catch (IOException e) {
            LOGGER.warn("cannot load properties from stream: {}", e.getMessage());
        }
        return configuration;
    }

    public static Configuration createStandardWithFile(File file) {
        standard = createWithFile(file, DEFAULT_HEADER, null);
        return standard;
    }

    public static Configuration createWithFile(File file, String header, DefinitionsProvider customProvider) {
        if (file == null) {
            throw new NullPointerException("file must not be null!");
        }
        Configuration configuration = new Configuration();
        configuration.apply(customProvider);
        if (file.exists()) {
            configuration.load(file);
        } else {
            configuration.store(file, header);
        }
        return configuration;
    }

    public Configuration() {
        this.values = new HashMap();
        this.transientValues = new HashSet();
        this.definitions = DEFAULT_DEFINITIONS;
        this.modules = DEFAULT_MODULES;
        applyModules();
    }

    public Configuration(Configuration config) {
        this.values = new HashMap();
        this.transientValues = new HashSet();
        this.definitions = DEFAULT_DEFINITIONS == config.definitions ? DEFAULT_DEFINITIONS : new Definitions<>(config.definitions);
        this.modules = DEFAULT_MODULES == config.modules ? DEFAULT_MODULES : new ConcurrentHashMap<>(config.modules);
        this.transientValues.addAll(config.transientValues);
        this.values.putAll(config.values);
    }

    public Configuration(ModuleDefinitionsProvider... providers) {
        this.values = new HashMap();
        this.transientValues = new HashSet();
        this.definitions = new Definitions<>("Configuration");
        this.modules = new ConcurrentHashMap();
        for (ModuleDefinitionsProvider provider : providers) {
            if (addModule(this.modules, provider)) {
                LOGGER.trace("added {}", provider.getModule());
            }
        }
        applyModules();
    }

    private void applyModules() {
        for (DefinitionsProvider handler : this.modules.values()) {
            handler.applyDefinitions(this);
        }
    }

    private void apply(DefinitionsProvider customProvider) {
        if (customProvider != null) {
            Set<String> before = new HashSet<>(this.modules.keySet());
            customProvider.applyDefinitions(this);
            if (before.size() < this.modules.size()) {
                Set<String> set = this.modules.keySet();
                set.removeAll(before);
                for (String newModule : set) {
                    LOGGER.warn("Add missing module {}", newModule);
                    this.modules.get(newModule).applyDefinitions(this);
                }
                customProvider.applyDefinitions(this);
            }
        }
    }

    public void load(File file) {
        if (file == null) {
            throw new NullPointerException("file must not be null");
        }
        LOGGER.info("loading properties from file {}", file.getAbsolutePath());
        try {
            InputStream inStream = new FileInputStream(file);
            try {
                load(inStream);
                inStream.close();
            } finally {
            }
        } catch (IOException e) {
            LOGGER.warn("cannot load properties from file {}: {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    public void load(InputStream inStream) throws IOException {
        if (inStream == null) {
            throw new NullPointerException("input stream must not be null");
        }
        Properties properties = new Properties();
        properties.load(inStream);
        add(properties);
    }

    public void add(Properties properties) {
        if (properties == null) {
            throw new NullPointerException("properties must not be null!");
        }
        for (Object k : properties.keySet()) {
            if (k instanceof String) {
                String key = (String) k;
                DocumentedDefinition<?> definition = (DocumentedDefinition) this.definitions.get(key);
                if (definition == null) {
                    LOGGER.warn("Ignore {}, no configuration definition available!", key);
                } else if (this.transientValues.contains(key)) {
                    LOGGER.info("Ignore {}, definition set transient!", key);
                } else {
                    String text = properties.getProperty(key);
                    Object value = loadValue(definition, text);
                    this.values.put(key, value);
                }
            }
        }
    }

    public void add(Dictionary<String, ?> dictionary) {
        if (dictionary == null) {
            throw new NullPointerException("dictionary must not be null!");
        }
        Enumeration<String> allKeys = dictionary.keys();
        while (allKeys.hasMoreElements()) {
            String key = allKeys.nextElement();
            Object value = dictionary.get(key);
            DocumentedDefinition<?> definition = (DocumentedDefinition) this.definitions.get(key);
            if (definition == null) {
                LOGGER.warn("Ignore {}, no configuration definition available!", key);
            } else if (this.transientValues.contains(key)) {
                LOGGER.info("Ignore {}, definition set transient!", key);
            } else {
                if (value instanceof String) {
                    String text = (String) value;
                    value = loadValue(definition, text);
                } else if (value != null) {
                    if (!definition.isAssignableFrom(value)) {
                        LOGGER.warn("{} is not a {}!", value.getClass().getSimpleName(), definition.getTypeName());
                        value = null;
                    }
                    try {
                        value = definition.checkRawValue(value);
                    } catch (ValueException e) {
                        value = null;
                    }
                }
                this.values.put(key, value);
            }
        }
    }

    private Object loadValue(DocumentedDefinition<?> definition, String text) {
        Object value = null;
        if (text != null) {
            String text2 = text.trim();
            if (!text2.isEmpty()) {
                try {
                    value = definition.readValue(text2);
                } catch (RuntimeException ex) {
                    LOGGER.warn("{}", ex.getMessage());
                    value = null;
                }
            }
        }
        return value;
    }

    public void store(File file) {
        store(file, DEFAULT_HEADER);
    }

    public void store(File file, String header) {
        if (file == null) {
            throw new NullPointerException("file must not be null");
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            try {
                store(out, header, file.getAbsolutePath());
                out.close();
            } finally {
            }
        } catch (IOException e) {
            LOGGER.warn("cannot write properties to {}: {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    public void store(OutputStream out, String header, String resourceName) {
        if (out == null) {
            throw new NullPointerException("output stream must not be null!");
        }
        if (header == null) {
            throw new NullPointerException("header must not be null!");
        }
        if (resourceName != null) {
            LOGGER.info("writing properties to {}", resourceName);
        }
        try {
            Set<String> modules = this.modules.keySet();
            List<String> generalKeys = new ArrayList<>();
            List<String> moduleKeys = new ArrayList<>();
            for (String key : this.values.keySet()) {
                if (!this.transientValues.contains(key)) {
                    boolean add = true;
                    Iterator<String> it = modules.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        String head = it.next();
                        if (key.startsWith(head)) {
                            moduleKeys.add(key);
                            add = false;
                            break;
                        }
                    }
                    if (add) {
                        generalKeys.add(key);
                    }
                }
            }
            Collections.sort(generalKeys);
            Collections.sort(moduleKeys);
            OutputStreamWriter fileWriter = new OutputStreamWriter(out);
            try {
                String line = PropertiesUtility.normalizeComments(header);
                fileWriter.write(line);
                fileWriter.write(StringUtil.lineSeparator());
                String line2 = PropertiesUtility.normalizeComments(new Date().toString());
                fileWriter.write(line2);
                fileWriter.write(StringUtil.lineSeparator());
                fileWriter.write(SslContextUtil.PARAMETER_SEPARATOR);
                fileWriter.write(StringUtil.lineSeparator());
                for (String key2 : generalKeys) {
                    writeProperty(key2, fileWriter);
                }
                for (String key3 : moduleKeys) {
                    writeProperty(key3, fileWriter);
                }
                fileWriter.close();
            } finally {
            }
        } catch (IOException e) {
            if (resourceName != null) {
                LOGGER.warn("cannot write properties to {}: {}", resourceName, e.getMessage());
            } else {
                LOGGER.warn("cannot write properties: {}", e.getMessage());
            }
        }
    }

    private void writeProperty(String key, Writer writer) throws IOException {
        String defaultText;
        DocumentedDefinition<? extends Object> definition = (DocumentedDefinition) this.definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Definition for " + key + " not found!");
        }
        StringBuilder documentation = new StringBuilder();
        String docu = definition.getDocumentation();
        if (docu != null) {
            documentation.append(docu);
        }
        Object defaultValue = definition.getDefaultValue();
        if (defaultValue != null && (defaultText = definition.write(defaultValue)) != null) {
            if (documentation.length() > 0) {
                documentation.append('\n');
            }
            documentation.append("Default: ").append(defaultText);
        }
        if (documentation.length() > 0) {
            String line = PropertiesUtility.normalizeComments(documentation.toString());
            writer.write(line);
            writer.write(StringUtil.lineSeparator());
        }
        String encoded = PropertiesUtility.normalize(key, true);
        writer.write(encoded);
        writer.write(61);
        Object value = this.values.get(key);
        if (value != null) {
            String encoded2 = PropertiesUtility.normalize(definition.write(value), false);
            writer.write(encoded2);
        }
        writer.write(StringUtil.lineSeparator());
    }

    public <T> Configuration setTransient(DocumentedDefinition<T> definition) {
        if (definition == null) {
            throw new NullPointerException("Definition must not be null!");
        }
        this.transientValues.add(definition.getKey());
        return this;
    }

    public <T> Configuration setFromText(DocumentedDefinition<T> definition, String value) {
        setInternal(definition, null, value);
        return this;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public <T> String getAsText(DocumentedDefinition<T> documentedDefinition) {
        return documentedDefinition.writeValue(getInternal(documentedDefinition));
    }

    public <T> Configuration set(BasicDefinition<T> definition, T value) {
        setInternal(definition, value, null);
        return this;
    }

    public <T> Configuration setAsList(BasicListDefinition<T> definition, T... values) {
        if (values == null) {
            throw new NullPointerException("Values must not be null!");
        }
        setInternal(definition, Arrays.asList(values), null);
        return this;
    }

    public <T> Configuration setAsListFromText(BasicListDefinition<T> definition, String... values) {
        if (values == null) {
            throw new NullPointerException("Values must not be null!");
        }
        if (values.length > 0) {
            StringBuffer all = new StringBuffer();
            for (String value : values) {
                all.append(value).append(ApiConstants.DELIMITER_COMMA);
            }
            int len = all.length();
            if (len > 0) {
                all.setLength(len - 1);
            }
            setInternal(definition, null, all.toString());
        } else {
            List<T> empty = Collections.emptyList();
            setInternal(definition, empty, null);
        }
        return this;
    }

    public <T> T get(BasicDefinition<T> basicDefinition) {
        return (T) getInternal(basicDefinition);
    }

    public Configuration set(TimeDefinition definition, Long value, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit must not be null");
        }
        if (value != null) {
            value = Long.valueOf(TimeUnit.NANOSECONDS.convert(value.longValue(), unit));
        }
        setInternal(definition, value, null);
        return this;
    }

    public Configuration set(TimeDefinition definition, int value, TimeUnit unit) {
        return set(definition, Long.valueOf(value), unit);
    }

    public Long get(TimeDefinition definition, TimeUnit unit) {
        Long time = (Long) getInternal(definition);
        if (unit == null) {
            throw new NullPointerException("unit must not be null");
        }
        if (time != null) {
            time = Long.valueOf(unit.convert(time.longValue(), TimeUnit.NANOSECONDS));
        }
        return time;
    }

    public int getTimeAsInt(TimeDefinition definition, TimeUnit unit) {
        Long time = get(definition, unit);
        if (time == null) {
            return 0;
        }
        if (time.longValue() > 2147483647L) {
            throw new IllegalArgumentException(time + " doesn't fit to int (Max. 2147483647)!");
        }
        if (time.longValue() < -2147483648L) {
            throw new IllegalArgumentException(time + " doesn't fit to int (Min. -2147483648)!");
        }
        return time.intValue();
    }

    private <T> T getInternal(DocumentedDefinition<T> documentedDefinition) {
        if (documentedDefinition == null) {
            throw new NullPointerException("definition must not be null");
        }
        DocumentedDefinition<T> documentedDefinition2 = (DocumentedDefinition) this.definitions.get(documentedDefinition.getKey());
        if (documentedDefinition2 != null && documentedDefinition2 != documentedDefinition) {
            throw new IllegalArgumentException("Definition " + documentedDefinition + " doesn't match " + documentedDefinition2);
        }
        T t = (T) this.values.get(documentedDefinition.getKey());
        if (t == null) {
            return documentedDefinition.getDefaultValue();
        }
        return t;
    }

    private <T> void setInternal(DocumentedDefinition<T> definition, T value, String text) {
        T value2;
        if (definition == null) {
            throw new NullPointerException("definition must not be null");
        }
        DocumentedDefinition<T> documentedDefinition = (DocumentedDefinition) this.definitions.addIfAbsent(definition);
        if (documentedDefinition != null && documentedDefinition != definition) {
            throw new IllegalArgumentException("Definition " + definition + " doesn't match " + documentedDefinition);
        }
        if (value == null && text != null) {
            value2 = definition.readValue(text);
        } else {
            if (value != null && !definition.isAssignableFrom(value)) {
                throw new IllegalArgumentException(value.getClass().getSimpleName() + " is not a " + definition.getTypeName());
            }
            try {
                value2 = definition.checkValue(value);
            } catch (ValueException ex) {
                throw new IllegalArgumentException(ex.getMessage());
            }
        }
        this.values.put(definition.getKey(), value2);
    }
}
