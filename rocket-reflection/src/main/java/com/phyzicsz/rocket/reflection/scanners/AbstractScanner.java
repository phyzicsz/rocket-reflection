

package com.phyzicsz.rocket.reflection.scanners;

import com.phyzicsz.rocket.reflection.Configuration;
import com.phyzicsz.rocket.reflection.ReflectionsException;
import com.phyzicsz.rocket.reflection.Store;
import com.phyzicsz.rocket.reflection.adapters.MetadataAdapter;
import com.phyzicsz.rocket.reflection.util.Utils;
import com.phyzicsz.rocket.reflection.vfs.Vfs;
import java.util.function.Predicate;

/**
 *
 */
@SuppressWarnings({"RawUseOfParameterizedType"})
public abstract class AbstractScanner implements Scanner {

    private Configuration configuration;
    private Predicate<String> resultFilter = s -> true; //accept all by default

    @Override
    public boolean acceptsInput(String file) {
        return getMetadataAdapter().acceptsInput(file);
    }

    @Override
    public Object scan(Vfs.File file, Object classObject, Store store) {
        if (classObject == null) {
            try {
                classObject = configuration.getMetadataAdapter().getOrCreateClassObject(file);
            } catch (Exception e) {
                throw new ReflectionsException("could not create class object from file " + file.getRelativePath(), e);
            }
        }
        scan(classObject, store);
        return classObject;
    }

    public abstract void scan(Object cls, Store store);

    protected void put(Store store, String key, String value) {
        store.put(Utils.index(getClass()), key, value);
    }

    //
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    public Predicate<String> getResultFilter() {
        return resultFilter;
    }

    public void setResultFilter(Predicate<String> resultFilter) {
        this.resultFilter = resultFilter;
    }

    @Override
    public Scanner filterResultsBy(Predicate<String> filter) {
        this.setResultFilter(filter);
        return this;
    }

    //
    @Override
    public boolean acceptResult(final String fqn) {
        return fqn != null && resultFilter.test(fqn);
    }

    protected MetadataAdapter getMetadataAdapter() {
        return configuration.getMetadataAdapter();
    }

    //
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof AbstractScanner);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
