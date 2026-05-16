package org.eclipse.californium.elements.util;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/DaemonThreadFactory.class */
public class DaemonThreadFactory extends NamedThreadFactory {
    public DaemonThreadFactory(String threadPrefix) {
        super(threadPrefix, null);
    }

    public DaemonThreadFactory(String threadPrefix, ThreadGroup threadGroup) {
        super(threadPrefix, threadGroup);
    }

    @Override // org.eclipse.californium.elements.util.NamedThreadFactory
    protected boolean createDaemonThreads() {
        return true;
    }
}
