package com.phyzicsz.rocket.reflection.scanners;

import com.phyzicsz.rocket.reflection.Store;

import java.util.List;

@SuppressWarnings({"unchecked"})
/** scans for method's annotations */
public class MethodAnnotationsScanner extends AbstractScanner {
    @Override
    public void scan(final Object cls, Store store) {
        for (Object method : getMetadataAdapter().getMethods(cls)) {
            for (String methodAnnotation : (List<String>) getMetadataAdapter().getMethodAnnotationNames(method)) {
                if (acceptResult(methodAnnotation)) {
                    put(store, methodAnnotation, getMetadataAdapter().getMethodFullKey(cls, method));
                }
            }
        }
    }
}
