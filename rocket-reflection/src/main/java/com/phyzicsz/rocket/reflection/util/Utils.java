package com.phyzicsz.rocket.reflection.util;

import static com.phyzicsz.rocket.reflection.util.ReflectionUtils.forName;
import com.phyzicsz.rocket.reflection.exception.ReflectionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a garbage can of convenient methods
 */
public abstract class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static String repeat(String string, int times) {
        return IntStream.range(0, times).mapToObj(i -> string).collect(Collectors.joining());
    }

    /**
     * isEmpty compatible with Java 5
     */
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static File prepareFile(String filename) {
        File file = new File(filename);
        File parent = file.getAbsoluteFile().getParentFile();
        if (!parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        return file;
    }

    public static Member getMemberFromDescriptor(String descriptor, ClassLoader... classLoaders) throws ReflectionException {
        int p0 = descriptor.lastIndexOf('(');
        String memberKey = p0 != -1 ? descriptor.substring(0, p0) : descriptor;
        String methodParameters = p0 != -1 ? descriptor.substring(p0 + 1, descriptor.lastIndexOf(')')) : "";

        int p1 = Math.max(memberKey.lastIndexOf('.'), memberKey.lastIndexOf("$"));
        String className = memberKey.substring(0, p1);
        String memberName = memberKey.substring(p1 + 1);

        Class<?>[] parameterTypes = null;
        if (!isEmpty(methodParameters)) {
            String[] parameterNames = methodParameters.split(",");
            parameterTypes = Arrays.stream(parameterNames).map(name -> forName(name.trim(), classLoaders)).toArray(Class<?>[]::new);
        }

        Class<?> aClass = forName(className, classLoaders);
        while (aClass != null) {
            try {
                if (!descriptor.contains("(")) {
                    return aClass.isInterface() ? aClass.getField(memberName) : aClass.getDeclaredField(memberName);
                } else if (isConstructor(descriptor)) {
                    return aClass.isInterface() ? aClass.getConstructor(parameterTypes) : aClass.getDeclaredConstructor(parameterTypes);
                } else {
                    return aClass.isInterface() ? aClass.getMethod(memberName, parameterTypes) : aClass.getDeclaredMethod(memberName, parameterTypes);
                }
            } catch (Exception e) {
                aClass = aClass.getSuperclass();
            }
        }
        throw new ReflectionException("Can't resolve member named " + memberName + " for class " + className);
    }

    public static Set<Method> getMethodsFromDescriptors(Iterable<String> annotatedWith, ClassLoader... classLoaders) {
        Set<Method> result = new HashSet<>();
        for (String annotated : annotatedWith) {
            if (!isConstructor(annotated)) {
                Method member = (Method) getMemberFromDescriptor(annotated, classLoaders);
                if (member != null) {
                    result.add(member);
                }
            }
        }
        return result;
    }

    public static Set<Constructor<?>> getConstructorsFromDescriptors(Iterable<String> annotatedWith, ClassLoader... classLoaders) {
        Set<Constructor<?>> result = new HashSet<>();
        for (String annotated : annotatedWith) {
            if (isConstructor(annotated)) {
                Constructor<?> member = (Constructor) getMemberFromDescriptor(annotated, classLoaders);
                if (member != null) {
                    result.add(member);
                }
            }
        }
        return result;
    }

    public static Set<Member> getMembersFromDescriptors(Iterable<String> values, ClassLoader... classLoaders) {
        Set<Member> result = new HashSet<>();
        for (String value : values) {
            try {
                result.add(Utils.getMemberFromDescriptor(value, classLoaders));
            } catch (ReflectionException e) {
                throw new ReflectionException("Can't resolve member named " + value, e);
            }
        }
        return result;
    }

    public static Field getFieldFromString(String field, ClassLoader... classLoaders) {
        String className = field.substring(0, field.lastIndexOf('.'));
        String fieldName = field.substring(field.lastIndexOf('.') + 1);

        try {
            return forName(className, classLoaders).getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new ReflectionException("Can't resolve field named " + fieldName, e);
        }
    }

    public static void close(InputStream closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            logger.warn("Could not close InputStream", e);
        }
    }

    public static boolean isConstructor(String fqn) {
        return fqn.contains("init>");
    }

    public static String name(Class<?> type) {
        if (!type.isArray()) {
            return type.getName();
        } else {
            int dim = 0;
            while (type.isArray()) {
                dim++;
                type = type.getComponentType();
            }
            return type.getName() + repeat("[]", dim);
        }
    }

    public static List<String> names(Collection<Class<?>> types) {
        return types.stream().map(Utils::name).collect(Collectors.toList());
    }

    public static List<String> names(Class<?>... types) {
        return names(Arrays.asList(types));
    }

    public static String name(Constructor<?> constructor) {
        return constructor.getName() + "." + "<init>" + "(" + join(names(constructor.getParameterTypes()), ", ") + ")";
    }

    public static String name(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName() + "(" + join(names(method.getParameterTypes()), ", ") + ")";
    }

    public static String name(Field field) {
        return field.getDeclaringClass().getName() + "." + field.getName();
    }

    public static String index(Class<?> scannerClass) {
        return scannerClass.getSimpleName();
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    public static <T> Predicate<T> and(Predicate... predicates) {
        return Arrays.stream(predicates).reduce(t -> true, Predicate<? super T>::and);
    }

    public static String join(Collection<?> elements, String delimiter) {
        return elements.stream().map(Object::toString).collect(Collectors.joining(delimiter));
    }

    @SafeVarargs
    public static <T> Set<T> filter(Collection<T> result, Predicate<? super T>... predicates) {
        return result.stream().filter(and(predicates)).collect(Collectors.toSet());
    }

    public static <T> Set<T> filter(Collection<T> result, Predicate<? super T> predicate) {
        return result.stream().filter(predicate).collect(Collectors.toSet());
    }

    @SafeVarargs
    public static <T> Set<T> filter(T[] result, Predicate<? super T>... predicates) {
        return Arrays.stream(result).filter(and(predicates)).collect(Collectors.toSet());
    }
}
