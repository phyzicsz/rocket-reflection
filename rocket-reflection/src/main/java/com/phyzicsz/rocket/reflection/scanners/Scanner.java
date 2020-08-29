package com.phyzicsz.rocket.reflection.scanners;

import com.phyzicsz.rocket.reflection.Configuration;
import com.phyzicsz.rocket.reflection.Store;
import com.phyzicsz.rocket.reflection.vfs.Vfs;

import java.util.function.Predicate;

/**
 *
 */
public interface Scanner {

    void setConfiguration(Configuration configuration);

    Scanner filterResultsBy(Predicate<String> filter);

    boolean acceptsInput(String file);

    Object scan(Vfs.File file, Object classObject, Store store);

    boolean acceptResult(String fqn);
}
