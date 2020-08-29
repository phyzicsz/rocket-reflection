

package com.phyzicsz.rocket.reflection;

import com.phyzicsz.rocket.reflection.adapters.MetadataAdapter;
import com.phyzicsz.rocket.reflection.scanners.Scanner;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;


public interface Configuration {

    /**
     * the scanner instances used for scanning different metadata
     *
     * @return
     */
    List<Scanner> getScanners();

    /**
     * the urls to be scanned
     *
     * @return
     */
    List<URL> getUrls();

    /**
     * the metadata adapter used to fetch metadata from classes
     *
     * @return
     */
    @SuppressWarnings({"RawUseOfParameterizedType"})
    MetadataAdapter<?,?,?> getMetadataAdapter();

    /**
     * get the fully qualified name filter used to filter types to be scanned
     *
     * @return
     */
    Predicate<String> getInputsFilter();

    /**
     * * executor service used to scan files.if null, scanning is done in a
     * simple for loop
     *
     * @return
     */
    ExecutorService getExecutorService();

    /**
     * get class loaders, might be used for resolving methods/fields
     *
     * @return
     */
    ClassLoader[] getClassLoaders();

    /**
     * if true (default), expand super types after scanning, for super types
     * that were not scanned.
     *
     * @return
     */
    boolean shouldExpandSuperTypes();
}
