package com.phyzicsz.rocket.reflection.scanners;

import com.phyzicsz.rocket.reflection.Store;
import com.phyzicsz.rocket.reflection.vfs.Vfs;

/** collects all resources that are not classes in a collection
 * <p>key: value - {web.xml: WEB-INF/web.xml} */
public class ResourcesScanner extends AbstractScanner {
    @Override
    public boolean acceptsInput(String file) {
        return !file.endsWith(".class") && !file.endsWith(".groovy") && !file.endsWith(".scala") && !file.endsWith(".kt"); //not a class
    }

    @Override public Object scan(Vfs.File file, Object classObject, Store store) {
        put(store, file.getName(), file.getRelativePath());
        return classObject;
    }

    @Override
    public void scan(Object cls, Store store) {
        throw new UnsupportedOperationException(); //shouldn't get here
    }
}