package com.phyzicsz.rocket.reflection;

import com.phyzicsz.rocket.reflection.util.ClasspathHelper;
import com.phyzicsz.rocket.reflection.util.ConfigurationBuilder;
import com.phyzicsz.rocket.reflection.util.FilterBuilder;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ReflectionsExpandSupertypesTest {

    private final static String packagePrefix =
            "com.phyzicsz.rocket.reflection.ReflectionsExpandSupertypesTest\\$TestModel\\$ScannedScope\\$.*";
    private FilterBuilder inputsFilter = new FilterBuilder().include(packagePrefix);

    public interface TestModel {
        interface A {
        } // outside of scanned scope

        interface B extends A {
        } // outside of scanned scope, but immediate supertype

        interface ScannedScope {
            interface C extends B {
            }

            interface D extends B {
            }
        }
    }

    @Test
    public void testExpandSupertypes() throws Exception {
        RocketReflection refExpand = new RocketReflection(new ConfigurationBuilder().
                setUrls(ClasspathHelper.forClass(TestModel.ScannedScope.C.class)).
                filterInputsBy(inputsFilter));
        assertTrue(refExpand.getConfiguration().shouldExpandSuperTypes());
        Set<Class<? extends TestModel.A>> subTypesOf = refExpand.getSubTypesOf(TestModel.A.class);
        assertTrue(subTypesOf.contains(TestModel.B.class));
        assertTrue(subTypesOf.containsAll(refExpand.getSubTypesOf(TestModel.B.class)));
    }

    @Test
    public void testNotExpandSupertypes() throws Exception {
        RocketReflection refDontExpand = new RocketReflection(new ConfigurationBuilder().
                setUrls(ClasspathHelper.forClass(TestModel.ScannedScope.C.class)).
                filterInputsBy(inputsFilter).
                setExpandSuperTypes(false));
        assertFalse(refDontExpand.getConfiguration().shouldExpandSuperTypes());
        Set<Class<? extends TestModel.A>> subTypesOf1 = refDontExpand.getSubTypesOf(TestModel.A.class);
        assertFalse(subTypesOf1.contains(TestModel.B.class));
    }
}
