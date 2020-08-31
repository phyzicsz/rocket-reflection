package com.phyzicsz.rocket.reflection;

import com.phyzicsz.rocket.reflection.util.ReflectionUtils;
import com.phyzicsz.rocket.reflection.MoreTestsModel.CyclicAnnotation;
import com.phyzicsz.rocket.reflection.MoreTestsModel.Meta;
import com.phyzicsz.rocket.reflection.MoreTestsModel.MultiName;
import com.phyzicsz.rocket.reflection.MoreTestsModel.Name;
import com.phyzicsz.rocket.reflection.MoreTestsModel.Names;
import com.phyzicsz.rocket.reflection.MoreTestsModel.ParamNames;
import com.phyzicsz.rocket.reflection.MoreTestsModel.SingleName;
import com.phyzicsz.rocket.reflection.scanners.MethodParameterNamesScanner;
import com.phyzicsz.rocket.reflection.scanners.ResourcesScanner;
import com.phyzicsz.rocket.reflection.scanners.SubTypesScanner;
import com.phyzicsz.rocket.reflection.scanners.TypeAnnotationsScanner;
import com.phyzicsz.rocket.reflection.util.ClasspathHelper;
import com.phyzicsz.rocket.reflection.util.ConfigurationBuilder;
import com.phyzicsz.rocket.reflection.util.FilterBuilder;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class MoreTests {

    @Test
    public void test_cyclic_annotation() {
        RocketReflection reflections = new RocketReflection(MoreTestsModel.class);
//        assertThat(reflections.getTypesAnnotatedWith(CyclicAnnotation.class),
//                are(CyclicAnnotation.class));
        
        assertThat(reflections.getTypesAnnotatedWith(CyclicAnnotation.class)).isNotNull();
        assertThat(reflections.getTypesAnnotatedWith(CyclicAnnotation.class)).contains(CyclicAnnotation.class);
    }

    @Test
    public void no_exception_when_configured_scanner_store_is_empty() {
        RocketReflection reflections = new RocketReflection(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("my.project.prefix"))
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
                .filterInputsBy(new FilterBuilder().includePackage("my.project.prefix")));

        reflections.getSubTypesOf(String.class);
    }

    @Test
    public void getAllAnnotated_returns_meta_annotations() {
        RocketReflection reflections = new RocketReflection(MoreTestsModel.class);
        for (Class<?> type: reflections.getTypesAnnotatedWith(Meta.class)) {
            Set<Annotation> allAnnotations = ReflectionUtils.getAllAnnotations(type);
            List<? extends Class<? extends Annotation>> collect = allAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toList());
            assertTrue(collect.contains(Meta.class));
        }

        Meta meta = new Meta() {
            @Override public String value() { return "a"; }
            @Override public Class<? extends Annotation> annotationType() { return Meta.class; }
        };
        for (Class<?> type: reflections.getTypesAnnotatedWith(meta)) {
            Set<Annotation> allAnnotations = ReflectionUtils.getAllAnnotations(type);
            List<? extends Class<? extends Annotation>> collect = allAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toList());
            assertTrue(collect.contains(Meta.class));
        }
    }

    @Test
    public void resources_scanner_filters_classes() {
        RocketReflection reflections = new RocketReflection(new ResourcesScanner());
        Set<String> keys = reflections.getStore().keys(ResourcesScanner.class.getSimpleName());
        assertTrue(keys.stream().noneMatch(res -> res.endsWith(".class")));
    }

    @Test
    public void test_repeatable() {
        RocketReflection ref = new RocketReflection(MoreTestsModel.class);
        Set<Class<?>> clazzes = ref.getTypesAnnotatedWith(Name.class);
        assertTrue(clazzes.contains(SingleName.class));
        assertFalse(clazzes.contains(MultiName.class));

        clazzes = ref.getTypesAnnotatedWith(Names.class);
        assertFalse(clazzes.contains(SingleName.class));
        assertTrue(clazzes.contains(MultiName.class));
    }

    @Test
    public void test_method_param_names_not_local_vars() throws NoSuchMethodException {
        RocketReflection reflections = new RocketReflection(MoreTestsModel.class, new MethodParameterNamesScanner());

        Class<ParamNames> clazz = ParamNames.class;
        assertEquals(reflections.getConstructorParamNames(clazz.getConstructor(String.class)).toString(),
                "[param1]");
        assertEquals(reflections.getMethodParamNames(clazz.getMethod("test", String.class, String.class)).toString(),
                "[testParam1, testParam2]");
        assertEquals(reflections.getMethodParamNames(clazz.getMethod("test", String.class)).toString(),
                "[testParam]");
        assertEquals(reflections.getMethodParamNames(clazz.getMethod("test2", String.class)).toString(),
                "[testParam]");

    }
}
