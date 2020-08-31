package com.phyzicsz.rocket.reflection;

import com.phyzicsz.rocket.reflection.util.ReflectionUtils;
import com.phyzicsz.rocket.reflection.exception.ReflectionException;
import static com.phyzicsz.rocket.reflection.util.ReflectionUtils.forName;
import static com.phyzicsz.rocket.reflection.util.ReflectionUtils.forNames;
import static com.phyzicsz.rocket.reflection.util.ReflectionUtils.withAnnotation;
import static com.phyzicsz.rocket.reflection.util.ReflectionUtils.withAnyParameterAnnotation;
import com.phyzicsz.rocket.reflection.scanners.FieldAnnotationsScanner;
import com.phyzicsz.rocket.reflection.scanners.MemberUsageScanner;
import com.phyzicsz.rocket.reflection.scanners.MethodAnnotationsScanner;
import com.phyzicsz.rocket.reflection.scanners.MethodParameterNamesScanner;
import com.phyzicsz.rocket.reflection.scanners.MethodParameterScanner;
import com.phyzicsz.rocket.reflection.scanners.ResourcesScanner;
import com.phyzicsz.rocket.reflection.scanners.Scanner;
import com.phyzicsz.rocket.reflection.scanners.SubTypesScanner;
import com.phyzicsz.rocket.reflection.scanners.TypeAnnotationsScanner;
import com.phyzicsz.rocket.reflection.util.ConfigurationBuilder;
import com.phyzicsz.rocket.reflection.util.Utils;
import com.phyzicsz.rocket.reflection.vfs.Vfs;
import static java.lang.String.format;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketReflection {

    private static final Logger logger = LoggerFactory.getLogger(RocketReflection.class);

    protected final transient Configuration configuration;
    protected Store store;

    public RocketReflection(final Configuration configuration) {
        this.configuration = configuration;
        store = new Store(configuration);

        if (configuration.getScanners() != null && !configuration.getScanners().isEmpty()) {
            //inject to scanners
            for (Scanner scanner : configuration.getScanners()) {
                scanner.setConfiguration(configuration);
            }

            scan();

            if (configuration.shouldExpandSuperTypes()) {
                expandSuperTypes();
            }
        }
    }

    public RocketReflection(final String prefix, final Scanner... scanners) {
        this((Object) prefix, scanners);
    }

    public RocketReflection(final Object... params) {
        this(ConfigurationBuilder.build(params));
    }

    protected RocketReflection() {
        configuration = new ConfigurationBuilder();
        store = new Store(configuration);
    }

    //
    protected void scan() {
        if (configuration.getUrls() == null || configuration.getUrls().isEmpty()) {

            logger.warn("given scan urls are empty. set urls in the configuration");

            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("going to scan these urls: {}", configuration.getUrls());
        }

        long time = System.currentTimeMillis();
        int scannedUrls = 0;
        ExecutorService executorService = configuration.getExecutorService();
        List<Future<?>> futures = new ArrayList<>();

        for (final URL url : configuration.getUrls()) {
            try {
                if (executorService != null) {
                    futures.add(executorService.submit(() -> {
                        if (logger.isTraceEnabled()) {
                            logger.trace("[{}] scanning {}", Thread.currentThread().toString(), url);
                        }
                        scan(url);
                    }));
                } else {
                    scan(url);
                }
                scannedUrls++;
            } catch (ReflectionException e) {

                logger.warn("could not create Vfs.Dir from url. ignoring the exception and continuing", e);

            }
        }

        if (executorService != null) {
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        //gracefully shutdown the parallel scanner executor service.
        if (executorService != null) {
            executorService.shutdown();
        }

        logger.info(format("Reflections took %d ms to scan %d urls, producing %s %s",
                System.currentTimeMillis() - time, scannedUrls, producingDescription(store),
                executorService instanceof ThreadPoolExecutor
                        ? format("[using %d cores]", ((ThreadPoolExecutor) executorService).getMaximumPoolSize()) : ""));

    }

    private static String producingDescription(Store store) {
        int keys = 0;
        int values = 0;
        for (String index : store.keySet()) {
            keys += store.keys(index).size();
            values += store.values(index).size();
        }
        return String.format("%d keys and %d values", keys, values);
    }

    protected void scan(URL url) {
        Vfs.Dir dir = Vfs.fromURL(url);

        try {
            for (final Vfs.File file : dir.getFiles()) {
                // scan if inputs filter accepts file relative path or fqn
                Predicate<String> inputsFilter = configuration.getInputsFilter();
                String path = file.getRelativePath();
                String fqn = path.replace('/', '.');
                if (inputsFilter == null || inputsFilter.test(path) || inputsFilter.test(fqn)) {
                    Object classObject = null;
                    for (Scanner scanner : configuration.getScanners()) {
                        try {
                            if (scanner.acceptsInput(path) || scanner.acceptsInput(fqn)) {
                                classObject = scanner.scan(file, classObject, store);
                            }
                        } catch (Exception e) {
                            if (logger.isTraceEnabled()) {

                                logger.trace("could not scan file {} in url {} with scanner {}", file.getRelativePath(), url.toExternalForm(), scanner.getClass().getSimpleName(), e);
                            }
                        }
                    }
                }
            }
        } finally {
            dir.close();
        }
    }

    /**
     * expand super types after scanning, for super types that were not scanned.
     * this is helpful in finding the transitive closure without scanning all
     * 3rd party dependencies. it uses
     * {@link ReflectionUtils#getSuperTypes(Class)}.
     * <p>
     * for example, for classes A,B,C where A supertype of B, B supertype of C:
     * <ul>
     * <li>if scanning C resulted in B (B->C in store), but A was not scanned
     * (although A supertype of B) - then getSubTypes(A) will not return C</li>
     * <li>if expanding supertypes, B will be expanded with A (A->B in store) -
     * then getSubTypes(A) will return C</li>
     * </ul>
     */
    public void expandSuperTypes() {
        String index = Utils.index(SubTypesScanner.class);
        Set<String> keys = store.keys(index);
        keys.removeAll(store.values(index));
        for (String key : keys) {
            final Class<?> type = forName(key, loaders());
            if (type != null) {
                expandSupertypes(store, key, type);
            }
        }
    }

    private void expandSupertypes(Store store, String key, Class<?> type) {
        for (Class<?> supertype : ReflectionUtils.getSuperTypes(type)) {
            if (store.put(SubTypesScanner.class, supertype.getName(), key)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("expanded subtype {} -> {}", supertype.getName(), key);
                }
                expandSupertypes(store, supertype.getName(), supertype);
            }
        }
    }

    //query
    /**
     * gets all sub types in hierarchy of a given type
     * <p/>
     * depends on SubTypesScanner configured
     */
    public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type) {
        return forNames(store.getAll(SubTypesScanner.class, type.getName()), loaders());
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>
     * {@link java.lang.annotation.Inherited} is not honored by default.
     * <p>
     * when honoring @Inherited, meta-annotation should only effect annotated
     * super classes and its sub types
     * <p>
     * <i>Note that this (@Inherited) meta-annotation type has no effect if the
     * annotated type is used for anything other then a class. Also, this
     * meta-annotation causes annotations to be inherited only from
     * superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>
     * depends on TypeAnnotationsScanner and SubTypesScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation) {
        return getTypesAnnotatedWith(annotation, false);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>
     * {@link java.lang.annotation.Inherited} is honored according to given
     * honorInherited.
     * <p>
     * when honoring @Inherited, meta-annotation should only effect annotated
     * super classes and it's sub types
     * <p>
     * when not honoring @Inherited, meta annotation effects all subtypes,
     * including annotations interfaces and classes
     * <p>
     * <i>Note that this (@Inherited) meta-annotation type has no effect if the
     * annotated type is used for anything other then a class. Also, this
     * meta-annotation causes annotations to be inherited only from
     * superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>
     * depends on TypeAnnotationsScanner and SubTypesScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation, boolean honorInherited) {
        Set<String> annotated = store.get(TypeAnnotationsScanner.class, annotation.getName());
        annotated.addAll(getAllAnnotated(annotated, annotation, honorInherited));
        return forNames(annotated, loaders());
    }

    /**
     * get types annotated with a given annotation, both classes and
     * annotations, including annotation member values matching
     * <p>
     * {@link java.lang.annotation.Inherited} is not honored by default
     * <p/>
     * depends on TypeAnnotationsScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation) {
        return getTypesAnnotatedWith(annotation, false);
    }

    /**
     * get types annotated with a given annotation, both classes and
     * annotations, including annotation member values matching
     * <p>
     * {@link java.lang.annotation.Inherited} is honored according to given
     * honorInherited
     * <p/>
     * depends on TypeAnnotationsScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation, boolean honorInherited) {
        Set<String> annotated = store.get(TypeAnnotationsScanner.class, annotation.annotationType().getName());
        Set<Class<?>> allAnnotated = Utils.filter(forNames(annotated, loaders()), withAnnotation(annotation));
        Set<Class<?>> classes = forNames(Utils.filter(getAllAnnotated(Utils.names(allAnnotated), annotation.annotationType(), honorInherited), s -> !annotated.contains(s)), loaders());
        allAnnotated.addAll(classes);
        return allAnnotated;
    }

    protected Collection<String> getAllAnnotated(Collection<String> annotated, Class<? extends Annotation> annotation, boolean honorInherited) {
        if (honorInherited) {
            if (annotation.isAnnotationPresent(Inherited.class)) {
                Set<String> subTypes = store.get(SubTypesScanner.class, Utils.filter(annotated, input -> {
                    final Class<?> type = forName(input, loaders());
                    return type != null && !type.isInterface();
                }));
                return store.getAllIncluding(SubTypesScanner.class, subTypes);
            } else {
                return annotated;
            }
        } else {
            Collection<String> subTypes = store.getAllIncluding(TypeAnnotationsScanner.class, annotated);
            return store.getAllIncluding(SubTypesScanner.class, subTypes);
        }
    }

    /**
     * get all methods annotated with a given annotation
     * <p/>
     * depends on MethodAnnotationsScanner configured
     */
    public Set<Method> getMethodsAnnotatedWith(final Class<? extends Annotation> annotation) {
        return Utils.getMethodsFromDescriptors(store.get(MethodAnnotationsScanner.class, annotation.getName()), loaders());
    }

    /**
     * get all methods annotated with a given annotation, including annotation
     * member values matching
     * <p/>
     * depends on MethodAnnotationsScanner configured
     */
    public Set<Method> getMethodsAnnotatedWith(final Annotation annotation) {
        return Utils.filter(getMethodsAnnotatedWith(annotation.annotationType()), withAnnotation(annotation));
    }

    /**
     * get methods with parameter types matching given {@code types}
     */
    public Set<Method> getMethodsMatchParams(Class<?>... types) {
        return Utils.getMethodsFromDescriptors(store.get(MethodParameterScanner.class, Utils.names(types).toString()), loaders());
    }

    /**
     * get methods with return type match given type
     */
    public Set<Method> getMethodsReturn(Class<?> returnType) {
        return Utils.getMethodsFromDescriptors(store.get(MethodParameterScanner.class, Utils.names(returnType)), loaders());
    }

    /**
     * get methods with any parameter annotated with given annotation
     */
    public Set<Method> getMethodsWithAnyParamAnnotated(Class<? extends Annotation> annotation) {
        return Utils.getMethodsFromDescriptors(store.get(MethodParameterScanner.class, annotation.getName()), loaders());

    }

    /**
     * get methods with any parameter annotated with given annotation, including
     * annotation member values matching
     */
    public Set<Method> getMethodsWithAnyParamAnnotated(Annotation annotation) {
        return Utils.filter(getMethodsWithAnyParamAnnotated(annotation.annotationType()), withAnyParameterAnnotation(annotation));
    }

    /**
     * get all constructors annotated with a given annotation
     * <p/>
     * depends on MethodAnnotationsScanner configured
     */
    public Set<Constructor<?>> getConstructorsAnnotatedWith(final Class<? extends Annotation> annotation) {
        return Utils.getConstructorsFromDescriptors(store.get(MethodAnnotationsScanner.class, annotation.getName()), loaders());
    }

    /**
     * get all constructors annotated with a given annotation, including
     * annotation member values matching
     * <p/>
     * depends on MethodAnnotationsScanner configured
     */
    public Set<Constructor<?>> getConstructorsAnnotatedWith(final Annotation annotation) {
        return Utils.filter(getConstructorsAnnotatedWith(annotation.annotationType()), withAnnotation(annotation));
    }

    /**
     * get constructors with parameter types matching given {@code types}
     */
    public Set<Constructor<?>> getConstructorsMatchParams(Class<?>... types) {
        return Utils.getConstructorsFromDescriptors(store.get(MethodParameterScanner.class, Utils.names(types).toString()), loaders());
    }

    /**
     * get constructors with any parameter annotated with given annotation
     */
    public Set<Constructor<?>> getConstructorsWithAnyParamAnnotated(Class<? extends Annotation> annotation) {
        return Utils.getConstructorsFromDescriptors(store.get(MethodParameterScanner.class, annotation.getName()), loaders());
    }

    /**
     * get constructors with any parameter annotated with given annotation,
     * including annotation member values matching
     */
    public Set<Constructor<?>> getConstructorsWithAnyParamAnnotated(Annotation annotation) {
        return Utils.filter(getConstructorsWithAnyParamAnnotated(annotation.annotationType()), withAnyParameterAnnotation(annotation));
    }

    /**
     * get all fields annotated with a given annotation
     * <p/>
     * depends on FieldAnnotationsScanner configured
     */
    public Set<Field> getFieldsAnnotatedWith(final Class<? extends Annotation> annotation) {
        return store.get(FieldAnnotationsScanner.class, annotation.getName()).stream()
                .map(annotated -> Utils.getFieldFromString(annotated, loaders()))
                .collect(Collectors.toSet());
    }

    /**
     * get all methods annotated with a given annotation, including annotation
     * member values matching
     * <p/>
     * depends on FieldAnnotationsScanner configured
     */
    public Set<Field> getFieldsAnnotatedWith(final Annotation annotation) {
        return Utils.filter(getFieldsAnnotatedWith(annotation.annotationType()), withAnnotation(annotation));
    }

    /**
     * get resources relative paths where simple name (key) matches given
     * namePredicate
     * <p>
     * depends on ResourcesScanner configured
     *
     */
    public Set<String> getResources(final Predicate<String> namePredicate) {
        Set<String> resources = Utils.filter(store.keys(Utils.index(ResourcesScanner.class)), namePredicate);
        return store.get(ResourcesScanner.class, resources);
    }

    /**
     * get resources relative paths where simple name (key) matches given
     * regular expression
     *
     */
    public Set<String> getResources(final Pattern pattern) {
        return getResources(input -> pattern.matcher(input).matches());
    }

    /**
     * get parameter names of given {@code method}
     * <p>
     * depends on MethodParameterNamesScanner configured
     */
    public List<String> getMethodParamNames(Method method) {
        Set<String> names = store.get(MethodParameterNamesScanner.class, Utils.name(method));
        return names.size() == 1 ? Arrays.asList(names.iterator().next().split(", ")) : Collections.emptyList();
    }

    /**
     * get parameter names of given {@code constructor}
     * <p>
     * depends on MethodParameterNamesScanner configured
     */
    public List<String> getConstructorParamNames(Constructor<?> constructor) {
        Set<String> names = store.get(MethodParameterNamesScanner.class, Utils.name(constructor));
        return names.size() == 1 ? Arrays.asList(names.iterator().next().split(", ")) : Collections.emptyList();
    }

    /**
     * get all given {@code field} usages in methods and constructors
     * <p>
     * depends on MemberUsageScanner configured
     */
    public Set<Member> getFieldUsage(Field field) {
        return Utils.getMembersFromDescriptors(store.get(MemberUsageScanner.class, Utils.name(field)));
    }

    /**
     * get all given {@code method} usages in methods and constructors
     * <p>
     * depends on MemberUsageScanner configured
     */
    public Set<Member> getMethodUsage(Method method) {
        return Utils.getMembersFromDescriptors(store.get(MemberUsageScanner.class, Utils.name(method)));
    }

    /**
     * get all given {@code constructors} usages in methods and constructors
     * <p>
     * depends on MemberUsageScanner configured
     */
    public Set<Member> getConstructorUsage(Constructor<?> cons) {
        return Utils.getMembersFromDescriptors(store.get(MemberUsageScanner.class, Utils.name(cons)));
    }

    /**
     * get all types scanned. this is effectively similar to getting all
     * subtypes of Object.
     * <p>
     * depends on SubTypesScanner configured with
     * {@code SubTypesScanner(false)}, otherwise {@code ReflectionsException} is
     * thrown
     * <p>
     * <i>note using this might be a bad practice. it is better to get types
     * matching some criteria, such as {@link #getSubTypesOf(Class)} or
     * {@link #getTypesAnnotatedWith(Class)}</i>
     *
     * @return Set of String, and not of Class, in order to avoid definition of
     * all types in PermGen
     */
    public Set<String> getAllTypes() {
        Set<String> allTypes = new HashSet<>(store.getAll(SubTypesScanner.class, Object.class.getName()));
        if (allTypes.isEmpty()) {
            throw new ReflectionException("Couldn't find subtypes of Object. "
                    + "Make sure SubTypesScanner initialized to include Object class - new SubTypesScanner(false)");
        }
        return allTypes;
    }

    /**
     * * returns the {@link com.phyzicsz.rocket.reflection.Store} used for
     * storing and querying the metadata
     */
    public Store getStore() {
        return store;
    }

    /**
     * Get the congifuration.
     *
     * @return Configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * serialize to a given directory and filename using given serializer
     * <p>
     * it is preferred to specify a designated directory (for example
     * META-INF/reflections), so that it could be found later much faster using
     * the load method
     */
//    public File save(final String filename, final Serializer serializer) {
//        return serializer.save(this, filename);
//    }
    private ClassLoader[] loaders() {
        return configuration.getClassLoaders();
    }
}
