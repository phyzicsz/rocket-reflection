package com.phyzicsz.rocket.reflection.util;

import com.phyzicsz.rocket.reflection.util.ConfigurationBuilder;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;



public class ConfigurationBuilderTest
{
    @Test
    public void testCallingAddClassLoaderMoreThanOnce()
    {
        ClassLoader fooClassLoader = new ClassLoader() { };
        ClassLoader barClassLoader = new ClassLoader() { };

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .addClassLoader( fooClassLoader  );

        // Attempt to add a second class loader
        configurationBuilder.addClassLoader( barClassLoader );
        assertTrue( true );
    }
}