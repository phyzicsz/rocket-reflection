package com.phyzicsz.rocket.reflection;

import com.phyzicsz.rocket.reflection.RocketReflection;
import com.phyzicsz.rocket.reflection.scanners.SubTypesScanner;
import com.phyzicsz.rocket.reflection.util.ClasspathHelper;
import com.phyzicsz.rocket.reflection.util.ConfigurationBuilder;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ReflectionsThreadSafenessTest {

    /**
     * https://github.com/ronmamo/reflections/issues/81
     */
    @Test
    public void reflections_scan_is_thread_safe() throws Exception {

        Callable<Set<Class<? extends Logger>>> callable = () -> {
            final RocketReflection reflections = new RocketReflection(new ConfigurationBuilder()
                    .setUrls(singletonList(ClasspathHelper.forClass(Logger.class)))
                    .setScanners(new SubTypesScanner(false)));

            return reflections.getSubTypesOf(Logger.class);
        };

        final ExecutorService pool = Executors.newFixedThreadPool(2);

        final Future<?> first = pool.submit(callable);
        final Future<?> second = pool.submit(callable);

        assertEquals(first.get(5, SECONDS), second.get(5, SECONDS));
    }
}
