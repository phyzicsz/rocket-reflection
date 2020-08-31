package com.phyzicsz.rocket.reflection;

import com.phyzicsz.rocket.reflection.exception.ReflectionException;
import com.phyzicsz.rocket.reflection.scanners.Scanner;
import static com.phyzicsz.rocket.reflection.util.Utils.index;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * stores metadata information in multimaps
 */
public class Store {

    private final ConcurrentHashMap<String, Map<String, Collection<String>>> storeMap;

    protected Store(Configuration configuration) {
        storeMap = new ConcurrentHashMap<>();
        for (Scanner scanner : configuration.getScanners()) {
            String index = index(scanner.getClass());
            storeMap.computeIfAbsent(index, s -> new ConcurrentHashMap<>());
        }
    }

    /**
     * return all indices
     *
     * @return keySet
     */
    public Set<String> keySet() {
        return storeMap.keySet();
    }

    /**
     * get the multimap object for the given {@code index}, otherwise throws a
     * ReflectionsException
     */
    private Map<String, Collection<String>> get(String index) {
        Map<String, Collection<String>> mmap = storeMap.get(index);
        if (mmap == null) {
            throw new ReflectionException("Scanner " + index + " was not configured");
        }
        return mmap;
    }

    /**
     * get the values stored for the given {@code index}
     *
     * @param scannerClass scanner class
     * @param key key
     * @return Set of values for key
     */
    public Set<String> get(Class<?> scannerClass, String key) {
        return get(index(scannerClass), Collections.singletonList(key));
    }

    /**
     * get the values stored for the given {@code index}
     *
     * @param index index
     * @param key key
     * @return set for index
     */
    public Set<String> get(String index, String key) {
        return get(index, Collections.singletonList(key));
    }

    /**
     * get the values stored for the given {@code index} and {@code keys}
     *
     * @param scannerClass the scanner class type
     * @param keys the keys
     * @return set set of values
     */
    public Set<String> get(Class<?> scannerClass, Collection<String> keys) {
        return get(index(scannerClass), keys);
    }

    /**
     * get the values stored for the given {@code index} and {@code keys}
     */
    private Set<String> get(String index, Collection<String> keys) {
        Map<String, Collection<String>> mmap = get(index);
        Set<String> result = new LinkedHashSet<>();
        for (String key : keys) {
            Collection<String> values = mmap.get(key);
            if (values != null) {
                result.addAll(values);
            }
        }
        return result;
    }

    /**
     * recursively get the values stored for the given {@code index} and
     * {@code keys}, including keys
     *
     * @param scannerClass the scanner class
     * @param keys the keys
     * @return the set of values
     */
    public Set<String> getAllIncluding(Class<?> scannerClass, Collection<String> keys) {
        String index = index(scannerClass);
        Map<String, Collection<String>> mmap = get(index);
        List<String> workKeys = new ArrayList<>(keys);

        Set<String> result = new HashSet<>();
        for (int i = 0; i < workKeys.size(); i++) {
            String key = workKeys.get(i);
            if (result.add(key)) {
                Collection<String> values = mmap.get(key);
                if (values != null) {
                    workKeys.addAll(values);
                }
            }
        }
        return result;
    }

    /**
     * recursively get the values stored for the given {@code index} not including keys
     *
     * @param scannerClass the scanner class
     * @param key the key
     * @return the set of values
     */
    public Set<String> getAll(Class<?> scannerClass, String key) {
        return getAllIncluding(scannerClass, get(scannerClass, key));
    }

    /**
     * recursively get the values stored for the given {@code index} not including keys
     *
     * @param scannerClass the scanner class
     * @param keys the keys
     * @return the set of values
     */
    public Set<String> getAll(Class<?> scannerClass, Collection<String> keys) {
        return getAllIncluding(scannerClass, get(scannerClass, keys));
    }

    public Set<String> keys(String index) {
        Map<String, Collection<String>> map = storeMap.get(index);
        return map != null ? new HashSet<>(map.keySet()) : Collections.emptySet();
    }

    public Set<String> values(String index) {
        Map<String, Collection<String>> map = storeMap.get(index);
        return map != null ? map.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()) : Collections.emptySet();
    }

    //
    public boolean put(Class<?> scannerClass, String key, String value) {
        return put(index(scannerClass), key, value);
    }

    public boolean put(String index, String key, String value) {
        return storeMap.computeIfAbsent(index, s -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, s -> Collections.synchronizedList(new ArrayList<>()))
                .add(value);
    }

    void merge(Store store) {
        if (store != null) {
            for (String indexName : store.keySet()) {
                Map<String, Collection<String>> index = store.get(indexName);
                if (index != null) {
                    for (String key : index.keySet()) {
                        for (String string : index.get(key)) {
                            put(indexName, key, string);
                        }
                    }
                }
            }
        }
    }
}
