package org.eclipse.californium.elements.config;

import java.util.concurrent.TimeUnit;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/SystemConfig.class */
public final class SystemConfig {
    public static final String MODULE = "SYS.";
    public static final TimeDefinition HEALTH_STATUS_INTERVAL = new TimeDefinition("SYS.HEALTH_STATUS_INTERVAL", "Health status interval. 0 to disable the health status.", 0, TimeUnit.SECONDS);
    public static final Configuration.ModuleDefinitionsProvider DEFINITIONS = new Configuration.ModuleDefinitionsProvider() { // from class: org.eclipse.californium.elements.config.SystemConfig.1
        @Override // org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider
        public String getModule() {
            return SystemConfig.MODULE;
        }

        @Override // org.eclipse.californium.elements.config.Configuration.DefinitionsProvider
        public void applyDefinitions(Configuration config) {
            config.set(SystemConfig.HEALTH_STATUS_INTERVAL, 0, TimeUnit.SECONDS);
        }
    };

    static {
        Configuration.addDefaultModule(DEFINITIONS);
    }

    public static void register() {
    }
}
