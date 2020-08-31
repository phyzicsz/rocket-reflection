package com.phyzicsz.rocket.reflection;

import com.phyzicsz.rocket.reflection.RocketReflection;
import com.phyzicsz.rocket.reflection.scanners.FieldAnnotationsScanner;
import com.phyzicsz.rocket.reflection.scanners.MemberUsageScanner;
import com.phyzicsz.rocket.reflection.scanners.MethodAnnotationsScanner;
import com.phyzicsz.rocket.reflection.scanners.MethodParameterNamesScanner;
import com.phyzicsz.rocket.reflection.scanners.MethodParameterScanner;
import com.phyzicsz.rocket.reflection.scanners.SubTypesScanner;
import com.phyzicsz.rocket.reflection.scanners.TypeAnnotationsScanner;
import com.phyzicsz.rocket.reflection.util.ClasspathHelper;
import com.phyzicsz.rocket.reflection.util.ConfigurationBuilder;

import java.util.Collections;
import org.junit.jupiter.api.BeforeAll;

/** */
public class ReflectionsParallelTest extends ReflectionsTest {

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
                        new MemberUsageScanner())
                .useParallelExecutor());
    }
}
