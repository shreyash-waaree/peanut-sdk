package org.eclipse.californium.core.config;

import com.keenon.common.error.PeanutError;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.elements.config.BooleanDefinition;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.EnumDefinition;
import org.eclipse.californium.elements.config.FloatDefinition;
import org.eclipse.californium.elements.config.IntegerDefinition;
import org.eclipse.californium.elements.config.StringSetDefinition;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.TimeDefinition;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/config/CoapConfig.class */
public final class CoapConfig {
    public static final String MODULE = "COAP.";
    public static final int DEFAULT_MAX_ACTIVE_PEERS = 150000;
    public static final long DEFAULT_MAX_PEER_INACTIVITY_PERIOD_IN_SECONDS = 600;
    public static final int DEFAULT_BLOCKWISE_STATUS_LIFETIME_IN_SECONDS = 300;
    public static final int DEFAULT_BLOCKWISE_STATUS_INTERVAL_IN_SECONDS = 5;
    public static final boolean DEFAULT_BLOCKWISE_STRICT_BLOCK2_OPTION = false;
    public static final boolean DEFAULT_BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER = true;
    public static final int DEFAULT_PREFERRED_BLOCK_SIZE = 512;
    public static final int DEFAULT_MAX_MESSAGE_SIZE = 1024;
    public static final int DEFAULT_MID_TRACKER_GROUPS = 16;
    public static final long DEFAULT_EXCHANGE_LIFETIME_IN_SECONDS = 247;
    public static final String DEDUPLICATOR_MARK_AND_SWEEP = "MARK_AND_SWEEP";
    public static final String DEFAULT_DEDUPLICATOR = "MARK_AND_SWEEP";
    public static final int DEFAULT_PEERS_MARK_AND_SWEEP_MESSAGES = 64;
    public static final long DEFAULT_MARK_AND_SWEEP_INTERVAL_IN_SECONDS = 10;
    public static final long DEFAULT_CROP_ROTATION_PERIOD_IN_SECONDS = 247;
    public static final boolean DEFAULT_DEDUPLICATOR_AUTO_REPLACE = true;
    public static final int DEFAULT_TOKEN_SIZE_LIMIT = 8;
    public static final TrackerMode DEFAULT_MID_TRACKER = TrackerMode.GROUPED;
    public static final MatcherMode DEFAULT_RESPONSE_MATCHING = MatcherMode.STRICT;
    public static final IntegerDefinition MAX_ACTIVE_PEERS = new IntegerDefinition("COAP.MAX_ACTIVE_PEERS", "Maximum number of active peers.", 150000, 1);
    public static final TimeDefinition MAX_PEER_INACTIVITY_PERIOD = new TimeDefinition("COAP.MAX_PEER_INACTIVITY_PERIOD", "Maximum inactive period of peer.", 600, TimeUnit.SECONDS);
    public static final IntegerDefinition COAP_PORT = new IntegerDefinition("COAP.COAP_PORT", "CoAP port.", 5683, 1);
    public static final IntegerDefinition COAP_SECURE_PORT = new IntegerDefinition("COAP.COAP_SECURE_PORT", "CoAP DTLS port.", Integer.valueOf(CoAP.DEFAULT_COAP_SECURE_PORT), 1);
    public static final TimeDefinition ACK_TIMEOUT = new TimeDefinition("COAP.ACK_TIMEOUT", "Initial CoAP acknowledge timeout.", 2000, TimeUnit.MILLISECONDS);
    public static final TimeDefinition MAX_ACK_TIMEOUT = new TimeDefinition("COAP.MAX_ACK_TIMEOUT", "Maximum CoAP acknowledge timeout.", 60000, TimeUnit.MILLISECONDS);
    public static final FloatDefinition ACK_INIT_RANDOM = new FloatDefinition("COAP.ACK_INIT_RANDOM", "Random factor for initial CoAP acknowledge timeout.", Float.valueOf(1.5f), Float.valueOf(1.0f));
    public static final FloatDefinition ACK_TIMEOUT_SCALE = new FloatDefinition("COAP.ACK_TIMEOUT_SCALE", "Scale factor for CoAP acknowledge backoff-timeout.", Float.valueOf(2.0f), Float.valueOf(1.0f));
    public static final IntegerDefinition MAX_RETRANSMIT = new IntegerDefinition("COAP.MAX_RETRANSMIT", "Maximum number of CoAP retransmissions.", 4, 1);
    public static final TimeDefinition EXCHANGE_LIFETIME = new TimeDefinition("COAP.EXCHANGE_LIFETIME", "CoAP maximum exchange lifetime for CON requests.", 247, TimeUnit.SECONDS);
    public static final TimeDefinition NON_LIFETIME = new TimeDefinition("COAP.NON_LIFETIME", "CoAP maximum lifetime for NON requests.", 145, TimeUnit.SECONDS);
    public static final TimeDefinition MAX_LATENCY = new TimeDefinition("COAP.MAX_LATENCY", "Maximum transmission latency for messages.", 100, TimeUnit.SECONDS);
    public static final TimeDefinition MAX_TRANSMIT_WAIT = new TimeDefinition("COAP.MAX_TRANSMIT_WAIT", "Maximum time to wait for ACK or RST after the first transmission of a CON message.", 93, TimeUnit.SECONDS);
    public static final TimeDefinition MAX_SERVER_RESPONSE_DELAY = new TimeDefinition("COAP.MAX_SERVER_RESPONSE_DELAY", "Maximum server response delay.", 250, TimeUnit.SECONDS);
    public static final IntegerDefinition NSTART = new IntegerDefinition("COAP.NSTART", "Maximum concurrent transmissions.", 1, 1);
    public static final TimeDefinition LEISURE = new TimeDefinition("COAP.LEISURE", "Timespan a multicast server may spread the response.", 5, TimeUnit.SECONDS);
    public static final FloatDefinition PROBING_RATE = new FloatDefinition("COAP.PROBING_RATE", "Probing rate to peers, which didn't response before. Currently not used.", Float.valueOf(1.0f));
    public static final BooleanDefinition USE_MESSAGE_OFFLOADING = new BooleanDefinition("COAP.USE_MESSAGE_OFFLOADING", "Use message off-loading, when data is not longer required.", false);
    public static final BooleanDefinition USE_RANDOM_MID_START = new BooleanDefinition("COAP.USE_RANDOM_MID_START", "Use initially a random value for MID.", true);
    public static final EnumDefinition<TrackerMode> MID_TRACKER = new EnumDefinition<>("COAP.MID_TACKER", "MID tracker.", TrackerMode.GROUPED, TrackerMode.values());
    public static final IntegerDefinition MID_TRACKER_GROUPS = new IntegerDefinition("COAP.MID_TRACKER_GROUPS", "Number of MID tracker groups.", 16, 4);
    public static final int DEFAULT_MULTICAST_BASE_MID = 65000;
    public static final IntegerDefinition MULTICAST_BASE_MID = new IntegerDefinition("COAP.MULTICAST_BASE_MID", "Base MID for multicast requests.", Integer.valueOf(DEFAULT_MULTICAST_BASE_MID), 0);
    public static final IntegerDefinition TOKEN_SIZE_LIMIT = new IntegerDefinition("COAP.TOKEN_SIZE_LIMIT", "Limit of token size.", 8, 1);
    public static final IntegerDefinition PREFERRED_BLOCK_SIZE = new IntegerDefinition("COAP.PREFERRED_BLOCK_SIZE", "Preferred blocksize for blockwise transfer.", 512, 16);
    public static final IntegerDefinition MAX_MESSAGE_SIZE = new IntegerDefinition("COAP.MAX_MESSAGE_SIZE", "Maximum payload size.", 1024, 16);
    public static final int DEFAULT_MAX_RESOURCE_BODY_SIZE = 8192;
    public static final IntegerDefinition MAX_RESOURCE_BODY_SIZE = new IntegerDefinition("COAP.MAX_RESOURCE_BODY_SIZE", "Maximum size of resource body. 0 to disable transparent blockwise mode.", Integer.valueOf(DEFAULT_MAX_RESOURCE_BODY_SIZE), 0);
    public static final TimeDefinition BLOCKWISE_STATUS_LIFETIME = new TimeDefinition("COAP.BLOCKWISE_STATUS_LIFETIME", "Lifetime of blockwise status.", 300, TimeUnit.SECONDS);
    public static final TimeDefinition BLOCKWISE_STATUS_INTERVAL = new TimeDefinition("COAP.BLOCKWISE_STATUS_INTERVAL", "Interval to validate lifetime of blockwise status.", 5, TimeUnit.SECONDS);
    public static final IntegerDefinition TCP_NUMBER_OF_BULK_BLOCKS = new IntegerDefinition("COAP.TCP_NUMBER_OF_BULK_BLOCKS", "Number of block per TCP-blockwise bulk transfer.", 1, 1);
    public static final BooleanDefinition BLOCKWISE_STRICT_BLOCK2_OPTION = new BooleanDefinition("COAP.BLOCKWISE_STRICT_BLOCK2_OPTION", "Use block2 option strictly, even if block2 is not required.", false);
    public static final BooleanDefinition BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER = new BooleanDefinition("COAP.BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER", "Enable automatic failover on \"entity too large\" response.", true);
    public static final TimeDefinition NOTIFICATION_CHECK_INTERVAL_TIME = new TimeDefinition("COAP.NOTIFICATION_CHECK_INTERVAL", "Interval time to check notifications receiver using a CON message.", 120, TimeUnit.SECONDS);
    public static final IntegerDefinition NOTIFICATION_CHECK_INTERVAL_COUNT = new IntegerDefinition("COAP.NOTIFICATION_CHECK_INTERVAL_COUNT", "Interval counter to check notifications receiver using a CON message.", 100);
    public static final TimeDefinition NOTIFICATION_REREGISTRATION_BACKOFF = new TimeDefinition("COAP.NOTIFICATION_REREGISTRATION_BACKOFF", "Additional time (backoff) to the max-age option\nfor waiting for the next notification before reregister.", 2000, TimeUnit.MILLISECONDS);
    public static final EnumDefinition<CongestionControlMode> CONGESTION_CONTROL_ALGORITHM = new EnumDefinition<>("COAP.CONGESTION_CONTROL_ALGORITHM", "Congestion-Control algorithm (still experimental).", CongestionControlMode.NULL, CongestionControlMode.values());
    public static final IntegerDefinition PROTOCOL_STAGE_THREAD_COUNT = new IntegerDefinition("COAP.PROTOCOL_STAGE_THREAD_COUNT", "Protocol stage thread count.", 1, 0);
    public static final String DEDUPLICATOR_PEERS_MARK_AND_SWEEP = "PEERS_MARK_AND_SWEEP";
    public static final String DEDUPLICATOR_CROP_ROTATION = "CROP_ROTATION";
    public static final String NO_DEDUPLICATOR = "NO_DEDUPLICATOR";
    public static final StringSetDefinition DEDUPLICATOR = new StringSetDefinition("COAP.DEDUPLICATOR", "Deduplicator algorithm.", "MARK_AND_SWEEP", "MARK_AND_SWEEP", DEDUPLICATOR_PEERS_MARK_AND_SWEEP, DEDUPLICATOR_CROP_ROTATION, NO_DEDUPLICATOR);
    public static final TimeDefinition MARK_AND_SWEEP_INTERVAL = new TimeDefinition("COAP.MARK_AND_SWEEP_INTERVAL", "Mark and sweep interval.", 10, TimeUnit.SECONDS);
    public static final IntegerDefinition PEERS_MARK_AND_SWEEP_MESSAGES = new IntegerDefinition("COAP.PEERS_MARK_AND_SWEEP_MESSAGES", "Maximum messages kept per peer for PEERS_MARK_AND_SWEEP.", 64, 4);
    public static final TimeDefinition CROP_ROTATION_PERIOD = new TimeDefinition("COAP.CROP_ROTATION_PERIOD", "Crop rotation period.", 247, TimeUnit.SECONDS);
    public static final BooleanDefinition DEDUPLICATOR_AUTO_REPLACE = new BooleanDefinition("COAP.DEDUPLICATOR_AUTO_REPLACE", "Automatic replace entries in deduplicator.", true);
    public static final EnumDefinition<MatcherMode> RESPONSE_MATCHING = new EnumDefinition<>("COAP.RESPONSE_MATCHING", "Response matching mode.", MatcherMode.STRICT, MatcherMode.values());
    public static final Configuration.ModuleDefinitionsProvider DEFINITIONS = new Configuration.ModuleDefinitionsProvider() { // from class: org.eclipse.californium.core.config.CoapConfig.1
        @Override // org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider
        public String getModule() {
            return CoapConfig.MODULE;
        }

        @Override // org.eclipse.californium.elements.config.Configuration.DefinitionsProvider
        public void applyDefinitions(Configuration config) {
            int CORES = Runtime.getRuntime().availableProcessors();
            config.set(CoapConfig.MAX_ACTIVE_PEERS, 150000);
            config.set(CoapConfig.MAX_PEER_INACTIVITY_PERIOD, (Long) 600L, TimeUnit.SECONDS);
            config.set(CoapConfig.COAP_PORT, 5683);
            config.set(CoapConfig.COAP_SECURE_PORT, Integer.valueOf(CoAP.DEFAULT_COAP_SECURE_PORT));
            config.set(CoapConfig.ACK_TIMEOUT, 2000, TimeUnit.MILLISECONDS);
            config.set(CoapConfig.ACK_INIT_RANDOM, Float.valueOf(1.5f));
            config.set(CoapConfig.ACK_TIMEOUT_SCALE, Float.valueOf(2.0f));
            config.set(CoapConfig.MAX_RETRANSMIT, 4);
            config.set(CoapConfig.EXCHANGE_LIFETIME, (Long) 247L, TimeUnit.SECONDS);
            config.set(CoapConfig.NON_LIFETIME, 145, TimeUnit.SECONDS);
            config.set(CoapConfig.NSTART, 1);
            config.set(CoapConfig.LEISURE, 5, TimeUnit.SECONDS);
            config.set(CoapConfig.PROBING_RATE, Float.valueOf(1.0f));
            config.set(CoapConfig.USE_MESSAGE_OFFLOADING, false);
            config.set(CoapConfig.MAX_LATENCY, 100, TimeUnit.SECONDS);
            config.set(CoapConfig.MAX_TRANSMIT_WAIT, 93, TimeUnit.SECONDS);
            config.set(CoapConfig.MAX_SERVER_RESPONSE_DELAY, 250, TimeUnit.SECONDS);
            config.set(CoapConfig.USE_RANDOM_MID_START, true);
            config.set(CoapConfig.MID_TRACKER, CoapConfig.DEFAULT_MID_TRACKER);
            config.set(CoapConfig.MID_TRACKER_GROUPS, 16);
            config.set(CoapConfig.TOKEN_SIZE_LIMIT, 8);
            config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 512);
            config.set(CoapConfig.MAX_MESSAGE_SIZE, 1024);
            config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, Integer.valueOf(CoapConfig.DEFAULT_MAX_RESOURCE_BODY_SIZE));
            config.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, 300, TimeUnit.SECONDS);
            config.set(CoapConfig.BLOCKWISE_STATUS_INTERVAL, 5, TimeUnit.SECONDS);
            config.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, false);
            config.set(CoapConfig.BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER, true);
            config.set(CoapConfig.TCP_NUMBER_OF_BULK_BLOCKS, 4);
            config.set(CoapConfig.NOTIFICATION_CHECK_INTERVAL_TIME, PeanutError.InterruptedIOException, TimeUnit.SECONDS);
            config.set(CoapConfig.NOTIFICATION_CHECK_INTERVAL_COUNT, 100);
            config.set(CoapConfig.NOTIFICATION_REREGISTRATION_BACKOFF, 2000, TimeUnit.MILLISECONDS);
            config.set(CoapConfig.CONGESTION_CONTROL_ALGORITHM, CongestionControlMode.NULL);
            config.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, Integer.valueOf(CORES));
            config.set(CoapConfig.DEDUPLICATOR, "MARK_AND_SWEEP");
            config.set(CoapConfig.MARK_AND_SWEEP_INTERVAL, (Long) 10L, TimeUnit.SECONDS);
            config.set(CoapConfig.PEERS_MARK_AND_SWEEP_MESSAGES, 64);
            config.set(CoapConfig.CROP_ROTATION_PERIOD, (Long) 247L, TimeUnit.SECONDS);
            config.set(CoapConfig.DEDUPLICATOR_AUTO_REPLACE, true);
            config.set(CoapConfig.RESPONSE_MATCHING, CoapConfig.DEFAULT_RESPONSE_MATCHING);
            config.set(CoapConfig.MULTICAST_BASE_MID, Integer.valueOf(CoapConfig.DEFAULT_MULTICAST_BASE_MID));
        }
    };

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/config/CoapConfig$CongestionControlMode.class */
    public enum CongestionControlMode {
        NULL,
        COCOA,
        COCOA_STRONG,
        BASIC_RTO,
        LINUX_RTO,
        PEAKHOPPER_RTO
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/config/CoapConfig$MatcherMode.class */
    public enum MatcherMode {
        STRICT,
        RELAXED,
        PRINCIPAL,
        PRINCIPAL_IDENTITY
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/config/CoapConfig$TrackerMode.class */
    public enum TrackerMode {
        NULL,
        GROUPED,
        MAPBASED
    }

    static {
        Configuration.addDefaultModule(DEFINITIONS);
    }

    public static void register() {
        SystemConfig.register();
    }
}
