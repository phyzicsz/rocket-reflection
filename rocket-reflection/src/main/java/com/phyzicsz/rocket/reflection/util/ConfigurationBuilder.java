package com.phyzicsz.rocket.reflection.util;

import com.phyzicsz.rocket.reflection.Configuration;
import com.phyzicsz.rocket.reflection.exception.ReflectionException;
import com.phyzicsz.rocket.reflection.adapters.JavassistAdapter;
import com.phyzicsz.rocket.reflection.adapters.MetadataAdapter;
import com.phyzicsz.rocket.reflection.scanners.Scanner;
import com.phyzicsz.rocket.reflection.scanners.SubTypesScanner;
import com.phyzicsz.rocket.reflection.scanners.TypeAnnotationsScanner;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a fluent builder forConfiguration
 */
public class ConfigurationBuilder implements Configuration {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationBuilder.class);

    private List<Scanner> scanners;
    private List<URL> urls;
    protected MetadataAdapter<?, ?, ?> metadataAdapter;
    private Predicate<String> inputsFilter;
    private ExecutorService executorService;
    private ClassLoader[] classLoaders;
    private boolean expandSuperTypes = true;

    public ConfigurationBuilder() {
        scanners = new ArrayList<>(Arrays.asList(new TypeAnnotationsScanner(), new SubTypesScanner()));
        urls = new ArrayList<>();
    }

    /**
     * constructs a {@link ConfigurationBuilder} using the given parameters, in
     * a non statically typed way. that is, each element in {@code params} is
     * guessed by it's type and populated into the configuration.
     * <ul>
     * <li>{@link String} - add urls using
     * {@link ClasspathHelper#forPackage(String, ClassLoader...)} ()}</li>
     * <li>{@link Class} - add urls using
     * {@link ClasspathHelper#forClass(Class, ClassLoader...)} </li>
     * <li>{@link ClassLoader} - use these classloaders in order to find urls in
     * ClasspathHelper.forPackage(), ClasspathHelper.forClass() and for
     * resolving types</li>
     * <li>{@link Scanner} - use given scanner, overriding the default
     * scanners</li>
     * <li>{@link URL} - add the given url for scanning</li>
     * <li>{@code Object[]} - flatten and use each element as above</li>
     * </ul>
     *
     * an input {@link FilterBuilder} will be set according to given packages.
     * <p>
     * use any parameter type in any order. this constructor uses instanceof on
     * each param and instantiate a {@link ConfigurationBuilder} appropriately.
     *
     */
    @SuppressWarnings("unchecked")
    public static ConfigurationBuilder build(final Object... params) {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        //flatten
        List<Object> parameters = new ArrayList<>();
        if (params != null) {
            for (Object param : params) {
                if (param != null) {
                    if (param.getClass().isArray()) {
                        for (Object p : (Object[]) param) {
                            if (p != null) {
                                parameters.add(p);
                            }
                        }
                    } else if (param instanceof Iterable) {
                        for (Object p : (Iterable) param) {
                            if (p != null) {
                                parameters.add(p);
                            }
                        }
                    } else {
                        parameters.add(param);
                    }
                }
            }
        }

        List<ClassLoader> loaders = new ArrayList<>();
        for (Object param : parameters) {
            if (param instanceof ClassLoader) {
                loaders.add((ClassLoader) param);
            }
        }

        ClassLoader[] classLoaders = loaders.isEmpty() ? null : loaders.toArray(new ClassLoader[loaders.size()]);
        FilterBuilder filter = new FilterBuilder();
        List<Scanner> scanners = new ArrayList<>();

        for (Object param : parameters) {
            if (param instanceof String) {
                builder.addUrls(ClasspathHelper.forPackage((String) param, classLoaders));
                filter.includePackage((String) param);
            } else if (param instanceof Class) {
                if (Scanner.class.isAssignableFrom((Class) param)) {
                    try {
                        builder.addScanners(((Scanner) ((Class) param).getDeclaredConstructor().newInstance()));
                    } catch (NoSuchMethodException
                            | SecurityException
                            | InstantiationException
                            | IllegalAccessException
                            | IllegalArgumentException
                            | InvocationTargetException ex) {
                        //fallback
                    }
                }
                builder.addUrls(ClasspathHelper.forClass((Class) param, classLoaders));
                filter.includePackage(((Class) param));
            } else if (param instanceof Scanner) {
                scanners.add((Scanner) param);
            } else if (param instanceof URL) {
                builder.addUrls((URL) param);
            } else if (param instanceof ClassLoader) {
                /* already taken care */ } else if (param instanceof Predicate) {
                filter.add((Predicate<String>) param);
            } else if (param instanceof ExecutorService) {
                builder.setExecutorService((ExecutorService) param);
            } else {
                throw new ReflectionException("could not use param " + param);
            }
        }

        if (builder.getUrls().isEmpty()) {
            if (classLoaders != null) {
                builder.addUrls(ClasspathHelper.forClassLoader(classLoaders)); //default urls getResources("")
            } else {
                builder.addUrls(ClasspathHelper.forClassLoader()); //default urls getResources("")
            }
            if (builder.urls.isEmpty()) {
                builder.addUrls(ClasspathHelper.forJavaClassPath());
            }
        }

        builder.filterInputsBy(filter);
        if (!scanners.isEmpty()) {
            builder.setScanners(scanners.toArray(new Scanner[scanners.size()]));
        }
        if (!loaders.isEmpty()) {
            builder.addClassLoaders(loaders);
        }

        return builder;
    }

    public ConfigurationBuilder forPackages(String... packages) {
        for (String pkg : packages) {
            addUrls(ClasspathHelper.forPackage(pkg));
        }
        return this;
    }

    @Override
    public List<Scanner> getScanners() {
        return scanners;
    }

    /**
     * set the scanners instances for scanning different metadata
     *
     * @param scanners the scanners to be added
     * @return ConfigurationBuilder
     */
    public ConfigurationBuilder setScanners(final Scanner... scanners) {
        this.scanners.clear();
        return addScanners(scanners);
    }

    /**
     * set the scanners instances for scanning different metadata
     *
     * @param scanners the scanners to be added
     * @return this
     */
    public ConfigurationBuilder addScanners(final Scanner... scanners) {
        this.scanners.addAll(Arrays.asList(scanners));
        return this;
    }

    @Override
    public List<URL> getUrls() {
        return urls;
    }

    /**
     * set the urls to be scanned
     * <p>
     * use {@link com.phyzicsz.rocket.reflection.util.ClasspathHelper}
     * convenient methods to get the relevant urls
     *
     */
    public ConfigurationBuilder setUrls(final Collection<URL> urls) {
        this.urls = new ArrayList<>(urls);
        return this;
    }

    /**
     * set the urls to be scanned
     * <p>
     * use {@link com.phyzicsz.rocket.reflection.util.ClasspathHelper}
     * convenient methods to get the relevant urls
     *
     */
    public ConfigurationBuilder setUrls(final URL... urls) {
        this.urls = new ArrayList<>(Arrays.asList(urls));
        return this;
    }

    /**
     * add urls to be scanned
     * <p>
     * use {@link com.phyzicsz.rocket.reflection.util.ClasspathHelper}
     * convenient methods to get the relevant urls
     *
     */
    public ConfigurationBuilder addUrls(final Collection<URL> urls) {
        this.urls.addAll(urls);
        return this;
    }

    /**
     * add urls to be scanned
     * <p>
     * use {@link com.phyzicsz.rocket.reflection.util.ClasspathHelper}
     * convenient methods to get the relevant urls
     *
     * @param urls URLs to be scanned
     * @return this
     */
    public ConfigurationBuilder addUrls(final URL... urls) {
        this.urls.addAll(new ArrayList<>(Arrays.asList(urls)));
        return this;
    }

    /**
     * returns the metadata adapter. if javassist library exists in the
     * classpath, this method returns {@link JavassistAdapter} otherwise
     * defaults to {@link JavaReflectionAdapter}.
     * <p>
     * the {@link JavassistAdapter} is preferred in terms of performance and
     * class loading.
     */
    @SuppressWarnings({"rawtypes"})
    @Override
    public MetadataAdapter getMetadataAdapter() {
        if (metadataAdapter != null) {
            return metadataAdapter;
        } else {
            return (metadataAdapter = new JavassistAdapter());
        }
    }

    /**
     * sets the metadata adapter used to fetch metadata from classes
     *
     * @param metadataAdapter the adapter to be added
     * @return this
     */
    @SuppressWarnings({"rawtypes"})
    public ConfigurationBuilder setMetadataAdapter(final MetadataAdapter metadataAdapter) {
        this.metadataAdapter = metadataAdapter;
        return this;
    }

    @Override
    public Predicate<String> getInputsFilter() {
        return inputsFilter;
    }

    /**
     * sets the input filter for all resources to be scanned.
     * <p>
     * supply a {@link Predicate} or use the {@link FilterBuilder
     *
     * @param inputsFilter}
     */
    public void setInputsFilter(Predicate<String> inputsFilter) {
        this.inputsFilter = inputsFilter;
    }

    /**
     * sets the input filter for all resources to be scanned.
     * <p>
     * supply a {@link Predicate} or use the {@link FilterBuilder
     *
     * @param inputsFilter}
     * @return this
     */
    public ConfigurationBuilder filterInputsBy(Predicate<String> inputsFilter) {
        this.inputsFilter = inputsFilter;
        return this;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * sets the executor service used for scanning.
     *
     * @param executorService the exec service used for scanning
     * @return ConfigurationBuilder
     */
    public ConfigurationBuilder setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * sets the executor service used for scanning to ThreadPoolExecutor with
     * core size as availableProcessors()
     *
     * <p>
     * default is ThreadPoolExecutor with a single core
     */
    public ConfigurationBuilder useParallelExecutor() {
        return useParallelExecutor(Runtime.getRuntime().availableProcessors());
    }

    /**
     * sets the executor service used for scanning to ThreadPoolExecutor with
     * core size as the given availableProcessors parameter. the executor
     * service spawns daemon threads by default.
     * <p>
     * default is ThreadPoolExecutor with a single core
     *
     * @param availableProcessors the number of availible processors
     * @return ConfigurationBuilder
     */
    public ConfigurationBuilder useParallelExecutor(final int availableProcessors) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("scanner-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        setExecutorService(Executors.newFixedThreadPool(availableProcessors, threadFactory));
        return this;
    }

    /**
     * get class loader, might be used for scanning or resolving methods/fields
     */
    @Override
    public ClassLoader[] getClassLoaders() {
        return classLoaders;
    }

    @Override
    public boolean shouldExpandSuperTypes() {
        return expandSuperTypes;
    }

    /**
     * if set to true, Reflections will expand super types after scanning.
     */
    public ConfigurationBuilder setExpandSuperTypes(boolean expandSuperTypes) {
        this.expandSuperTypes = expandSuperTypes;
        return this;
    }

    /**
     * set class loader, might be used for resolving methods/fields
     */
    public ConfigurationBuilder setClassLoaders(ClassLoader[] classLoaders) {
        this.classLoaders = classLoaders;
        return this;
    }

    /**
     * add class loader, might be used for resolving methods/fields
     */
    public ConfigurationBuilder addClassLoader(ClassLoader classLoader) {
        return addClassLoaders(classLoader);
    }

    /**
     * add class loader, might be used for resolving methods/fields
     */
    public ConfigurationBuilder addClassLoaders(ClassLoader... classLoaders) {
        this.classLoaders = this.classLoaders == null
                ? classLoaders
                : Stream.concat(Arrays.stream(this.classLoaders), Arrays.stream(classLoaders)).toArray(ClassLoader[]::new);
        return this;
    }

    /**
     * add class loader, might be used for resolving methods/fields
     */
    public ConfigurationBuilder addClassLoaders(Collection<ClassLoader> classLoaders) {
        return addClassLoaders(classLoaders.toArray(new ClassLoader[classLoaders.size()]));
    }
}
