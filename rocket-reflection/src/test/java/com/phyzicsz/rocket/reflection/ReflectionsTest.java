package com.phyzicsz.rocket.reflection;


import com.phyzicsz.rocket.reflection.util.ReflectionUtils;
import com.phyzicsz.rocket.reflection.exception.ReflectionException;
import com.phyzicsz.rocket.reflection.TestModel.AC1;
import com.phyzicsz.rocket.reflection.TestModel.AC1n;
import com.phyzicsz.rocket.reflection.TestModel.AC2;
import com.phyzicsz.rocket.reflection.TestModel.AC3;
import com.phyzicsz.rocket.reflection.TestModel.AF1;
import com.phyzicsz.rocket.reflection.TestModel.AI1;
import com.phyzicsz.rocket.reflection.TestModel.AI2;
import com.phyzicsz.rocket.reflection.TestModel.AM1;
import com.phyzicsz.rocket.reflection.TestModel.C1;
import com.phyzicsz.rocket.reflection.TestModel.C2;
import com.phyzicsz.rocket.reflection.TestModel.C3;
import com.phyzicsz.rocket.reflection.TestModel.C4;
import com.phyzicsz.rocket.reflection.TestModel.C5;
import com.phyzicsz.rocket.reflection.TestModel.C6;
import com.phyzicsz.rocket.reflection.TestModel.C7;
import com.phyzicsz.rocket.reflection.TestModel.I1;
import com.phyzicsz.rocket.reflection.TestModel.I2;
import com.phyzicsz.rocket.reflection.TestModel.I3;
import com.phyzicsz.rocket.reflection.TestModel.MAI1;
import com.phyzicsz.rocket.reflection.TestModel.Usage;
import com.phyzicsz.rocket.reflection.scanners.FieldAnnotationsScanner;
import com.phyzicsz.rocket.reflection.scanners.MemberUsageScanner;
import com.phyzicsz.rocket.reflection.scanners.MethodAnnotationsScanner;
import com.phyzicsz.rocket.reflection.scanners.MethodParameterNamesScanner;
import com.phyzicsz.rocket.reflection.scanners.MethodParameterScanner;
import com.phyzicsz.rocket.reflection.scanners.SubTypesScanner;
import com.phyzicsz.rocket.reflection.scanners.TypeAnnotationsScanner;
import com.phyzicsz.rocket.reflection.util.ClasspathHelper;
import com.phyzicsz.rocket.reflection.util.ConfigurationBuilder;
import com.phyzicsz.rocket.reflection.util.FilterBuilder;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ReflectionsTest {
    public static final FilterBuilder TestModelFilter = new FilterBuilder().include("com.phyzicsz.rocket.reflection.TestModel\\$.*");
    static RocketReflection reflections;

    @BeforeAll
    public static void init() {
        reflections = new RocketReflection(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new FieldAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner()));
    }

    @Test
    public void testSubTypesOf() {
        assertThat(reflections.getSubTypesOf(I1.class), are(I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getSubTypesOf(C1.class), are(C2.class, C3.class, C5.class));

        assertFalse(reflections.getAllTypes().isEmpty());
    }

    @Test
    public void testTypesAnnotatedWith() {
        assertThat(reflections.getTypesAnnotatedWith(MAI1.class, true), are(AI1.class));
        assertThat(reflections.getTypesAnnotatedWith(MAI1.class, true), annotatedWith(MAI1.class));

        assertThat(reflections.getTypesAnnotatedWith(AI2.class, true), are(I2.class));
        assertThat(reflections.getTypesAnnotatedWith(AI2.class, true), annotatedWith(AI2.class));

        assertThat(reflections.getTypesAnnotatedWith(AC1.class, true), are(C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getTypesAnnotatedWith(AC1.class, true), annotatedWith(AC1.class));

        assertThat(reflections.getTypesAnnotatedWith(AC1n.class, true), are(C1.class));
        assertThat(reflections.getTypesAnnotatedWith(AC1n.class, true), annotatedWith(AC1n.class));

        assertThat(reflections.getTypesAnnotatedWith(MAI1.class), are(AI1.class, I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getTypesAnnotatedWith(MAI1.class), metaAnnotatedWith(MAI1.class));

        assertThat(reflections.getTypesAnnotatedWith(AI1.class), are(I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getTypesAnnotatedWith(AI1.class), metaAnnotatedWith(AI1.class));

        assertThat(reflections.getTypesAnnotatedWith(AI2.class), are(I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getTypesAnnotatedWith(AI2.class), metaAnnotatedWith(AI2.class));

        assertThat(reflections.getTypesAnnotatedWith(AM1.class), isEmpty);

        //annotation member value matching
        AC2 ac2 = new AC2() {
            public String value() {return "ugh?!";}
            public Class<? extends Annotation> annotationType() {return AC2.class;}};

        assertThat(reflections.getTypesAnnotatedWith(ac2), are(C3.class, C5.class, I3.class, C6.class, AC3.class, C7.class));

        assertThat(reflections.getTypesAnnotatedWith(ac2, true), are(C3.class, I3.class, AC3.class));
    }

    @Test
    public void testMethodsAnnotatedWith() {
        try {
            assertThat(reflections.getMethodsAnnotatedWith(AM1.class),
                    are(C4.class.getDeclaredMethod("m1"),
                        C4.class.getDeclaredMethod("m1", int.class, String[].class),
                        C4.class.getDeclaredMethod("m1", int[][].class, String[][].class),
                        C4.class.getDeclaredMethod("m3")));

            AM1 am1 = new AM1() {
                public String value() {return "1";}
                public Class<? extends Annotation> annotationType() {return AM1.class;}
            };
            assertThat(reflections.getMethodsAnnotatedWith(am1),
                    are(C4.class.getDeclaredMethod("m1"),
                        C4.class.getDeclaredMethod("m1", int.class, String[].class),
                        C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test
    public void testConstructorsAnnotatedWith() {
        try {
            assertThat(reflections.getConstructorsAnnotatedWith(AM1.class),
                    are(C4.class.getDeclaredConstructor(String.class)));

            AM1 am1 = new AM1() {
                public String value() {return "1";}
                public Class<? extends Annotation> annotationType() {return AM1.class;}
            };
            assertThat(reflections.getConstructorsAnnotatedWith(am1),
                    are(C4.class.getDeclaredConstructor(String.class)));
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test
    public void testFieldsAnnotatedWith() {
        try {
            assertThat(reflections.getFieldsAnnotatedWith(AF1.class),
                    are(C4.class.getDeclaredField("f1"),
                        C4.class.getDeclaredField("f2")
                        ));

            assertThat(reflections.getFieldsAnnotatedWith(new AF1() {
                            public String value() {return "2";}
                            public Class<? extends Annotation> annotationType() {return AF1.class;}}),
                    are(C4.class.getDeclaredField("f2")));
        } catch (NoSuchFieldException e) {
            fail();
        }
    }

    @Test
    public void testMethodParameter() {
        try {
            assertThat(reflections.getMethodsMatchParams(String.class),
                    are(C4.class.getDeclaredMethod("m4", String.class), Usage.C1.class.getDeclaredMethod("method", String.class)));

            assertThat(reflections.getMethodsMatchParams(),
                    are(C4.class.getDeclaredMethod("m1"), C4.class.getDeclaredMethod("m3"),
                            AC2.class.getMethod("value"), AF1.class.getMethod("value"), AM1.class.getMethod("value"),
                            Usage.C1.class.getDeclaredMethod("method"), Usage.C2.class.getDeclaredMethod("method")));

            assertThat(reflections.getMethodsMatchParams(int[][].class, String[][].class),
                    are(C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));

            assertThat(reflections.getMethodsReturn(int.class),
                    are(C4.class.getDeclaredMethod("add", int.class, int.class)));

            assertThat(reflections.getMethodsReturn(String.class),
                    are(C4.class.getDeclaredMethod("m3"), C4.class.getDeclaredMethod("m4", String.class),
                            AC2.class.getMethod("value"), AF1.class.getMethod("value"), AM1.class.getMethod("value")));

            assertThat(reflections.getMethodsReturn(void.class),
                    are(C4.class.getDeclaredMethod("m1"), C4.class.getDeclaredMethod("m1", int.class, String[].class),
                            C4.class.getDeclaredMethod("m1", int[][].class, String[][].class), Usage.C1.class.getDeclaredMethod("method"),
                            Usage.C1.class.getDeclaredMethod("method", String.class), Usage.C2.class.getDeclaredMethod("method")));

            assertThat(reflections.getMethodsWithAnyParamAnnotated(AM1.class),
                    are(C4.class.getDeclaredMethod("m4", String.class)));

            assertThat(reflections.getMethodsWithAnyParamAnnotated(
                    new AM1() {
                        public String value() { return "2"; }
                        public Class<? extends Annotation> annotationType() { return AM1.class; }
                    }),
                    are(C4.class.getDeclaredMethod("m4", String.class)));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testConstructorParameter() throws NoSuchMethodException {
        assertThat(reflections.getConstructorsMatchParams(String.class),
                are(C4.class.getDeclaredConstructor(String.class)));

        assertThat(reflections.getConstructorsMatchParams(),
                are(C1.class.getDeclaredConstructor(), C2.class.getDeclaredConstructor(), C3.class.getDeclaredConstructor(),
                        C4.class.getDeclaredConstructor(), C5.class.getDeclaredConstructor(), C6.class.getDeclaredConstructor(),
                        C7.class.getDeclaredConstructor(), Usage.C1.class.getDeclaredConstructor(), Usage.C2.class.getDeclaredConstructor()));

        assertThat(reflections.getConstructorsWithAnyParamAnnotated(AM1.class),
                are(C4.class.getDeclaredConstructor(String.class)));

        assertThat(reflections.getConstructorsWithAnyParamAnnotated(
                new AM1() {
                    public String value() { return "1"; }
                    public Class<? extends Annotation> annotationType() { return AM1.class; }
                }),
                are(C4.class.getDeclaredConstructor(String.class)));
    }

//    @Test
//    public void testResourcesScanner() {
//        Predicate<String> filter = new FilterBuilder().include(".*\\.xml").exclude(".*testModel-reflections\\.xml");
//        Reflections reflections = new Reflections(new ConfigurationBuilder()
//                .filterInputsBy(filter)
//                .setScanners(new ResourcesScanner())
//                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class))));
//
//        Set<String> resolved = reflections.getResources(Pattern.compile(".*resource1-reflections\\.xml"));
//        assertThat(resolved, are("META-INF/reflections/resource1-reflections.xml"));
//
//        Set<String> resources = reflections.getStore().keys(index(ResourcesScanner.class));
//        assertThat(resources, are("resource1-reflections.xml", "resource2-reflections.xml"));
//    }

    @Test
    public void testMethodParameterNames() throws NoSuchMethodException {
        assertEquals(reflections.getMethodParamNames(C4.class.getDeclaredMethod("m3")),
                Collections.emptyList());

        assertEquals(reflections.getMethodParamNames(C4.class.getDeclaredMethod("m4", String.class)),
                Collections.singletonList("string"));

        assertEquals(reflections.getMethodParamNames(C4.class.getDeclaredMethod("add", int.class, int.class)),
                Arrays.asList("i1", "i2"));

        assertEquals(reflections.getConstructorParamNames(C4.class.getDeclaredConstructor(String.class)),
                Collections.singletonList("f1"));
    }

    @Test
    public void testMemberUsageScanner() throws NoSuchFieldException, NoSuchMethodException {
        //field usage
        assertThat(reflections.getFieldUsage(Usage.C1.class.getDeclaredField("c2")),
                are(Usage.C1.class.getDeclaredConstructor(),
                        Usage.C1.class.getDeclaredConstructor(Usage.C2.class),
                        Usage.C1.class.getDeclaredMethod("method"),
                        Usage.C1.class.getDeclaredMethod("method", String.class)));

        //method usage
        assertThat(reflections.getMethodUsage(Usage.C1.class.getDeclaredMethod("method")),
                are(Usage.C2.class.getDeclaredMethod("method")));

        assertThat(reflections.getMethodUsage(Usage.C1.class.getDeclaredMethod("method", String.class)),
                are(Usage.C2.class.getDeclaredMethod("method")));

        //constructor usage
        assertThat(reflections.getConstructorUsage(Usage.C1.class.getDeclaredConstructor()),
                are(Usage.C2.class.getDeclaredConstructor(),
                        Usage.C2.class.getDeclaredMethod("method")));

        assertThat(reflections.getConstructorUsage(Usage.C1.class.getDeclaredConstructor(Usage.C2.class)),
                are(Usage.C2.class.getDeclaredMethod("method")));
    }

    @Test
    public void testScannerNotConfigured() {
        try {
            new RocketReflection(TestModel.class, TestModelFilter).getMethodsAnnotatedWith(AC1.class);
            fail();
        } catch (ReflectionException e) {
            assertEquals(e.getMessage(), "Scanner " + MethodAnnotationsScanner.class.getSimpleName() + " was not configured");
        }
    }

    //
    public static String getUserDir() {
        File file = new File(System.getProperty("user.dir"));
        //a hack to fix user.dir issue(?) in surfire
        if (Arrays.asList(file.list()).contains("reflections")) {
            file = new File(file, "reflections");
        }
        return file.getAbsolutePath();
    }

    private final BaseMatcher<Set<Class<?>>> isEmpty = new BaseMatcher<Set<Class<?>>>() {
        public boolean matches(Object o) {
            return ((Collection<?>) o).isEmpty();
        }

        public void describeTo(Description description) {
            description.appendText("empty collection");
        }
    };

    private abstract static class Match<T> extends BaseMatcher<T> {
        public void describeTo(Description description) { }
    }

    public static <T> Matcher<Set<? super T>> are(final T... ts) {
        final Collection<?> c1 = Arrays.asList(ts);
        return new Match<Set<? super T>>() {
            public boolean matches(Object o) {
                Collection<?> c2 = (Collection<?>) o;
                return c1.containsAll(c2) && c2.containsAll(c1);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(Arrays.toString(ts));
            }
        };
    }

    private Matcher<Set<Class<?>>> annotatedWith(final Class<? extends Annotation> annotation) {
        return new Match<Set<Class<?>>>() {
            public boolean matches(Object o) {
                for (Class<?> c : (Iterable<Class<?>>) o) {
                    if (!annotationTypes(Arrays.asList(c.getAnnotations())).contains(annotation)) return false;
                }
                return true;
            }
        };
    }

    private Matcher<Set<Class<?>>> metaAnnotatedWith(final Class<? extends Annotation> annotation) {
        return new Match<Set<Class<?>>>() {
            public boolean matches(Object o) {
                for (Class<?> c : (Iterable<Class<?>>) o) {
                    Set<Class> result = new HashSet<>();
                    List<Class> stack = new ArrayList<>(ReflectionUtils.getAllSuperTypes(c));
                    while (!stack.isEmpty()) {
                        Class next = stack.remove(0);
                        if (result.add(next)) {
                            for (Class<? extends Annotation> ac : annotationTypes(Arrays.asList(next.getDeclaredAnnotations()))) {
                                if (!result.contains(ac) && !stack.contains(ac)) stack.add(ac);
                            }
                        }
                    }
                    if (!result.contains(annotation)) return false;
                }
                return true;
            }
        };
    }

    private List<Class<? extends Annotation>> annotationTypes(Collection<Annotation> annotations) {
        return annotations.stream().filter(Objects::nonNull).map(Annotation::annotationType).collect(Collectors.toList());
    }
}
