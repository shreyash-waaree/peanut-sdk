package org.eclipse.californium.elements.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/LeastRecentlyUsedCache.class */
public class LeastRecentlyUsedCache<K, V> {
    public static final int DEFAULT_INITIAL_CAPACITY = 16;
    public static final long DEFAULT_THRESHOLD_SECS = 1800;
    public static final int DEFAULT_CAPACITY = 150000;
    private Collection<V> values;
    private final Map<K, CacheEntry<K, V>> cache;
    private volatile int capacity;
    private CacheEntry<K, V> header;
    private volatile long expirationThresholdNanos;
    private volatile boolean evictOnReadAccess;
    private volatile boolean updateOnReadAccess;
    private final List<EvictionListener<V>> evictionListeners;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/LeastRecentlyUsedCache$EvictionListener.class */
    public interface EvictionListener<V> {
        void onEviction(V v);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/LeastRecentlyUsedCache$Predicate.class */
    public interface Predicate<V> {
        boolean accept(V v);
    }

    public LeastRecentlyUsedCache() {
        this(16, 150000, DEFAULT_THRESHOLD_SECS, TimeUnit.SECONDS);
    }

    public LeastRecentlyUsedCache(int capacity, long threshold) {
        this(Math.min(capacity, 16), capacity, threshold, TimeUnit.SECONDS);
    }

    public LeastRecentlyUsedCache(int initialCapacity, int maxCapacity, long threshold) {
        this(initialCapacity, maxCapacity, threshold, TimeUnit.SECONDS);
    }

    public LeastRecentlyUsedCache(int initialCapacity, int maxCapacity, long threshold, TimeUnit unit) {
        this.evictOnReadAccess = true;
        this.updateOnReadAccess = true;
        this.evictionListeners = new LinkedList();
        if (initialCapacity > maxCapacity) {
            throw new IllegalArgumentException("initial capacity must be <= max capacity");
        }
        this.capacity = maxCapacity;
        this.cache = new ConcurrentHashMap(initialCapacity);
        setExpirationThreshold(threshold, unit);
        initLinkedList();
    }

    private void initLinkedList() {
        this.header = new CacheEntry<>();
        CacheEntry<K, V> cacheEntry = this.header;
        ((CacheEntry) cacheEntry).after = ((CacheEntry) this.header).before = this.header;
    }

    public void addEvictionListener(EvictionListener<V> listener) {
        if (listener != null) {
            this.evictionListeners.add(listener);
        }
    }

    public boolean isEvictingOnReadAccess() {
        return this.evictOnReadAccess;
    }

    public void setEvictingOnReadAccess(boolean evict) {
        this.evictOnReadAccess = evict;
    }

    public boolean isUpdatingOnReadAccess() {
        return this.updateOnReadAccess;
    }

    public void setUpdatingOnReadAccess(boolean update) {
        this.updateOnReadAccess = update;
    }

    public final long getExpirationThreshold() {
        return TimeUnit.NANOSECONDS.toSeconds(this.expirationThresholdNanos);
    }

    public final void setExpirationThreshold(long newThreshold) {
        setExpirationThreshold(newThreshold, TimeUnit.SECONDS);
    }

    public final void setExpirationThreshold(long newThreshold, TimeUnit unit) {
        this.expirationThresholdNanos = unit.toNanos(newThreshold);
    }

    public final int getCapacity() {
        return this.capacity;
    }

    public final void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public final int size() {
        return this.cache.size();
    }

    public final int remainingCapacity() {
        return Math.max(0, this.capacity - this.cache.size());
    }

    public final void clear() {
        this.cache.clear();
        initLinkedList();
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final boolean put(K key, V value) {
        if (value != null) {
            CacheEntry<K, V> existingEntry = this.cache.get(key);
            if (existingEntry == null) {
                if (this.cache.size() < this.capacity) {
                    add(key, value);
                    return true;
                }
                CacheEntry<K, V> eldest = ((CacheEntry) this.header).after;
                if (!eldest.isStale(this.expirationThresholdNanos)) {
                    return false;
                }
                eldest.remove();
                this.cache.remove(eldest.getKey());
                add(key, value);
                notifyEvictionListeners(eldest.getValue());
                return true;
            }
            existingEntry.remove();
            add(key, value);
            return true;
        }
        return false;
    }

    private void notifyEvictionListeners(V session) {
        for (EvictionListener<V> listener : this.evictionListeners) {
            listener.onEviction(session);
        }
    }

    final V getEldest() {
        return (V) ((CacheEntry) this.header).after.getValue();
    }

    private void add(K key, V value) {
        CacheEntry<K, V> entry = new CacheEntry<>(key, value);
        this.cache.put(key, entry);
        entry.addBefore(this.header);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final boolean put(K key, V value, long lastUpdate) {
        if (value != null) {
            CacheEntry<K, V> existingEntry = this.cache.get(key);
            if (existingEntry == null) {
                if (this.cache.size() < this.capacity) {
                    add(key, value, lastUpdate);
                    return true;
                }
                CacheEntry<K, V> eldest = ((CacheEntry) this.header).after;
                if (!eldest.isStale(this.expirationThresholdNanos) || lastUpdate - ((CacheEntry) eldest).lastUpdate < 0) {
                    return false;
                }
                eldest.remove();
                this.cache.remove(eldest.getKey());
                add(key, value, lastUpdate);
                notifyEvictionListeners(eldest.getValue());
                return true;
            }
            existingEntry.remove();
            add(key, value, lastUpdate);
            return true;
        }
        return false;
    }

    private void add(K key, V value, long lastUpdate) {
        CacheEntry<K, V> entry = new CacheEntry<>(key, value, lastUpdate);
        this.cache.put(key, entry);
        if (((CacheEntry) this.header).before != this.header) {
            CacheEntry<K, V> position = this.header;
            while (lastUpdate - ((CacheEntry) position).before.lastUpdate < 0) {
                position = ((CacheEntry) position).before;
                if (position == this.header) {
                    break;
                }
            }
            entry.addBefore(position);
            return;
        }
        entry.addBefore(this.header);
    }

    public final V get(K key) {
        CacheEntry<K, V> entry;
        if (key == null || (entry = this.cache.get(key)) == null) {
            return null;
        }
        return access(entry, null);
    }

    public final Timestamped<V> getTimestamped(K key) {
        CacheEntry<K, V> entry;
        if (key == null || (entry = this.cache.get(key)) == null) {
            return null;
        }
        Timestamped<V> timestamped = entry.getEntry();
        if (access(entry, null) == null) {
            return null;
        }
        return timestamped;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    public final V access(CacheEntry<K, V> cacheEntry, Iterator<CacheEntry<K, V>> it) {
        if (this.evictOnReadAccess && this.expirationThresholdNanos > 0 && cacheEntry.isStale(this.expirationThresholdNanos)) {
            if (it != null) {
                it.remove();
            } else {
                this.cache.remove(cacheEntry.getKey());
            }
            cacheEntry.remove();
            notifyEvictionListeners(cacheEntry.getValue());
            return null;
        }
        if (this.updateOnReadAccess) {
            cacheEntry.recordAccess(this.header);
        }
        return (V) cacheEntry.getValue();
    }

    public final boolean update(K key) {
        CacheEntry<K, V> entry;
        if (key == null || (entry = this.cache.get(key)) == null) {
            return false;
        }
        entry.recordAccess(this.header);
        return true;
    }

    public final V remove(K k) {
        CacheEntry<K, V> cacheEntryRemove;
        if (k == null || (cacheEntryRemove = this.cache.remove(k)) == null) {
            return null;
        }
        cacheEntryRemove.remove();
        return (V) cacheEntryRemove.getValue();
    }

    public final V remove(K key, V value) {
        CacheEntry<K, V> entry;
        if (key != null && (entry = this.cache.get(key)) != null && entry.getValue() == value) {
            this.cache.remove(key);
            entry.remove();
            return value;
        }
        return null;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final int removeExpiredEntries(int maxEntries) {
        CacheEntry<K, V> eldest;
        int counter = 0;
        while (true) {
            if ((maxEntries != 0 && counter >= maxEntries) || this.header == (eldest = ((CacheEntry) this.header).after) || !eldest.isStale(this.expirationThresholdNanos)) {
                break;
            }
            eldest.remove();
            this.cache.remove(eldest.getKey());
            notifyEvictionListeners(eldest.getValue());
            counter++;
        }
        return counter;
    }

    public final V find(Predicate<V> predicate) {
        return find(predicate, true);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final V find(Predicate<V> predicate, boolean unique) {
        if (predicate != 0) {
            Iterator<CacheEntry<K, V>> iterator = this.cache.values().iterator();
            while (iterator.hasNext()) {
                CacheEntry<K, V> entry = iterator.next();
                if (predicate.accept(entry.getValue())) {
                    V value = access(entry, iterator);
                    if (unique || value != null) {
                        return value;
                    }
                }
            }
            return null;
        }
        return null;
    }

    public final Iterator<V> valuesIterator() {
        return valuesIterator(true);
    }

    public final Iterator<V> valuesIterator(final boolean readAccess) {
        final Iterator<CacheEntry<K, V>> iterator = this.cache.values().iterator();
        return new Iterator<V>() { // from class: org.eclipse.californium.elements.util.LeastRecentlyUsedCache.1
            private boolean hasNextCalled;
            private CacheEntry<K, V> nextEntry;

            /* JADX WARN: Code restructure failed: missing block: B:13:0x0042, code lost:
            
                r4.nextEntry = r0;
             */
            @Override // java.util.Iterator
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct code enable 'Show inconsistent code' option in preferences
            */
            public boolean hasNext() {
                /*
                    r4 = this;
                    r0 = r4
                    boolean r0 = r0.hasNextCalled
                    if (r0 != 0) goto L69
                    r0 = r4
                    r1 = 0
                    r0.nextEntry = r1
                Lc:
                    r0 = r4
                    java.util.Iterator r0 = r5
                    boolean r0 = r0.hasNext()
                    if (r0 == 0) goto L64
                    r0 = r4
                    java.util.Iterator r0 = r5
                    java.lang.Object r0 = r0.next()
                    org.eclipse.californium.elements.util.LeastRecentlyUsedCache$CacheEntry r0 = (org.eclipse.californium.elements.util.LeastRecentlyUsedCache.CacheEntry) r0
                    r5 = r0
                    r0 = r4
                    boolean r0 = r6
                    if (r0 == 0) goto L59
                    r0 = r4
                    org.eclipse.californium.elements.util.LeastRecentlyUsedCache r0 = org.eclipse.californium.elements.util.LeastRecentlyUsedCache.this
                    r1 = r0
                    r6 = r1
                    monitor-enter(r0)
                    r0 = r4
                    org.eclipse.californium.elements.util.LeastRecentlyUsedCache r0 = org.eclipse.californium.elements.util.LeastRecentlyUsedCache.this     // Catch: java.lang.Throwable -> L51
                    r1 = r5
                    r2 = r4
                    java.util.Iterator r2 = r5     // Catch: java.lang.Throwable -> L51
                    java.lang.Object r0 = org.eclipse.californium.elements.util.LeastRecentlyUsedCache.access$1300(r0, r1, r2)     // Catch: java.lang.Throwable -> L51
                    if (r0 == 0) goto L4c
                    r0 = r4
                    r1 = r5
                    r0.nextEntry = r1     // Catch: java.lang.Throwable -> L51
                    r0 = r6
                    monitor-exit(r0)     // Catch: java.lang.Throwable -> L51
                    goto L64
                L4c:
                    r0 = r6
                    monitor-exit(r0)     // Catch: java.lang.Throwable -> L51
                    goto L56
                L51:
                    r7 = move-exception
                    r0 = r6
                    monitor-exit(r0)     // Catch: java.lang.Throwable -> L51
                    r0 = r7
                    throw r0
                L56:
                    goto L61
                L59:
                    r0 = r4
                    r1 = r5
                    r0.nextEntry = r1
                    goto L64
                L61:
                    goto Lc
                L64:
                    r0 = r4
                    r1 = 1
                    r0.hasNextCalled = r1
                L69:
                    r0 = r4
                    org.eclipse.californium.elements.util.LeastRecentlyUsedCache$CacheEntry<K, V> r0 = r0.nextEntry
                    if (r0 == 0) goto L74
                    r0 = 1
                    goto L75
                L74:
                    r0 = 0
                L75:
                    return r0
                */
                throw new UnsupportedOperationException("Method not decompiled: org.eclipse.californium.elements.util.LeastRecentlyUsedCache.AnonymousClass1.hasNext():boolean");
            }

            @Override // java.util.Iterator
            public V next() {
                hasNext();
                this.hasNextCalled = false;
                if (this.nextEntry == null) {
                    throw new NoSuchElementException();
                }
                return (V) ((CacheEntry) this.nextEntry).value;
            }

            @Override // java.util.Iterator
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public final Collection<V> values() {
        Collection<V> vs = this.values;
        if (vs == null) {
            vs = new AbstractCollection<V>() { // from class: org.eclipse.californium.elements.util.LeastRecentlyUsedCache.2
                @Override // java.util.AbstractCollection, java.util.Collection
                public final int size() {
                    return LeastRecentlyUsedCache.this.cache.size();
                }

                @Override // java.util.AbstractCollection, java.util.Collection
                public final boolean contains(final Object o) {
                    return null != LeastRecentlyUsedCache.this.find(new Predicate<V>() { // from class: org.eclipse.californium.elements.util.LeastRecentlyUsedCache.2.1
                        @Override // org.eclipse.californium.elements.util.LeastRecentlyUsedCache.Predicate
                        public boolean accept(V value) {
                            return value.equals(o);
                        }
                    }, false);
                }

                @Override // java.util.AbstractCollection, java.util.Collection, java.lang.Iterable
                public final Iterator<V> iterator() {
                    return LeastRecentlyUsedCache.this.valuesIterator();
                }

                @Override // java.util.AbstractCollection, java.util.Collection
                public final boolean add(Object o) {
                    throw new UnsupportedOperationException();
                }

                @Override // java.util.AbstractCollection, java.util.Collection
                public final boolean remove(Object o) {
                    throw new UnsupportedOperationException();
                }

                @Override // java.util.AbstractCollection, java.util.Collection
                public final void clear() {
                    throw new UnsupportedOperationException();
                }
            };
            this.values = vs;
        }
        return vs;
    }

    public final Iterator<Timestamped<V>> timestampedIterator() {
        return new Iterator<Timestamped<V>>() { // from class: org.eclipse.californium.elements.util.LeastRecentlyUsedCache.3
            final int max;
            int counter;
            CacheEntry<K, V> current;

            {
                this.max = LeastRecentlyUsedCache.this.cache.size();
                this.current = LeastRecentlyUsedCache.this.header;
            }

            @Override // java.util.Iterator
            public boolean hasNext() {
                return ((CacheEntry) this.current).after != LeastRecentlyUsedCache.this.header && this.counter < this.max;
            }

            @Override // java.util.Iterator
            public Timestamped<V> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                this.counter++;
                this.current = ((CacheEntry) this.current).after;
                return this.current.getEntry();
            }

            @Override // java.util.Iterator
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/LeastRecentlyUsedCache$CacheEntry.class */
    private static class CacheEntry<K, V> {
        private final K key;
        private final V value;
        private long lastUpdate;
        private CacheEntry<K, V> after;
        private CacheEntry<K, V> before;

        private CacheEntry() {
            this.key = null;
            this.value = null;
            this.lastUpdate = -1L;
        }

        private CacheEntry(K key, V value) {
            this(key, value, ClockUtil.nanoRealtime());
        }

        private CacheEntry(K key, V value, long lasUpdate) {
            this.key = key;
            this.value = value;
            this.lastUpdate = lasUpdate;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Timestamped<V> getEntry() {
            return new Timestamped<>(this.value, this.lastUpdate);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public K getKey() {
            return this.key;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public V getValue() {
            return this.value;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isStale(long thresholdNanos) {
            return ClockUtil.nanoRealtime() - this.lastUpdate >= thresholdNanos;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void recordAccess(CacheEntry<K, V> header) {
            remove();
            this.lastUpdate = ClockUtil.nanoRealtime();
            addBefore(header);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addBefore(CacheEntry<K, V> existingEntry) {
            this.after = existingEntry;
            this.before = existingEntry.before;
            this.before.after = this;
            this.after.before = this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void remove() {
            this.before.after = this.after;
            this.after.before = this.before;
        }

        public String toString() {
            return "CacheEntry [key: " + this.key + ", last access: " + this.lastUpdate + "]";
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/LeastRecentlyUsedCache$Timestamped.class */
    public static final class Timestamped<V> {
        private final V value;
        private final long lastUpdate;

        public Timestamped(V value, long lastUpdate) {
            this.value = value;
            this.lastUpdate = lastUpdate;
        }

        public V getValue() {
            return this.value;
        }

        public long getLastUpdate() {
            return this.lastUpdate;
        }

        public int hashCode() {
            int hash = (int) (this.lastUpdate ^ (this.lastUpdate >>> 32));
            if (this.value != null) {
                return hash + this.value.hashCode();
            }
            return hash;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Timestamped<?> other = (Timestamped) obj;
            if (this.lastUpdate != other.lastUpdate) {
                return false;
            }
            if (this.value == null) {
                return other.value == null;
            }
            return this.value.equals(other.value);
        }

        public String toString() {
            return this.lastUpdate + ": " + this.value;
        }
    }
}
