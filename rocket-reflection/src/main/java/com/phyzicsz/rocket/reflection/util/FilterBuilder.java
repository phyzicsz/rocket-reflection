package com.phyzicsz.rocket.reflection.util;

import com.google.common.base.Splitter;
import com.phyzicsz.rocket.reflection.exception.ReflectionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class FilterBuilder implements Predicate<String> {

    private final List<Predicate<String>> chain;

    public FilterBuilder() {
        chain = new ArrayList<>();
    }

    private FilterBuilder(final Collection<Predicate<String>> filters) {
        chain = new ArrayList<>(filters);
    }

    /**
     * include a regular expression
     */
    public FilterBuilder include(final String regex) {
        return add(new Include(regex));
    }

    /**
     * exclude a regular expression
     */
    public FilterBuilder exclude(final String regex) {
        add(new Exclude(regex));
        return this;
    }

    /**
     * add a Predicate to the chain of predicates
     */
    public FilterBuilder add(Predicate<String> filter) {
        chain.add(filter);
        return this;
    }

    /**
     * include a package of a given class
     */
    public FilterBuilder includePackage(final Class<?> aClass) {
        return add(new Include(packageNameRegex(aClass)));
    }

    /**
     * exclude a package of a given class
     */
    public FilterBuilder excludePackage(final Class<?> aClass) {
        return add(new Exclude(packageNameRegex(aClass)));
    }

    /**
     * include packages of given prefixes
     */
    public FilterBuilder includePackage(final String... prefixes) {
        for (String prefix : prefixes) {
            add(new Include(prefix(prefix)));
        }
        return this;
    }

    /**
     * exclude packages of a given prefix
     */
    public FilterBuilder excludePackage(final String... prefixes) {
        for (String prefix : prefixes) {
            add(new Exclude(prefix(prefix)));
        }
        return this;
    }

    private static String packageNameRegex(Class<?> aClass) {
        return prefix(aClass.getPackage().getName() + ".");
    }

    public static String prefix(String qualifiedName) {
        return qualifiedName.replace(".", "\\.") + ".*";
    }

    @Override
    public String toString() {
        return Utils.join(chain, ", ");
    }

    @Override
    public boolean test(String regex) {
        boolean accept = chain.isEmpty() || chain.get(0) instanceof Exclude;

        for (Predicate<String> filter : chain) {
            if (accept && filter instanceof Include) {
                continue;
            } //skip if this filter won't change
            if (!accept && filter instanceof Exclude) {
                continue;
            }
            accept = filter.test(regex);
            if (!accept && filter instanceof Exclude) {
                break;
            } //break on first exclusion
        }
        return accept;
    }

    public abstract static class Matcher implements Predicate<String> {

        final Pattern pattern;

        protected Matcher(final String regex) {
            pattern = Pattern.compile(regex);
        }

        @Override
        public String toString() {
            return pattern.pattern();
        }
    }

    public static class Include extends Matcher {

        public Include(final String patternString) {
            super(patternString);
        }

        @Override
        public boolean test(final String regex) {
            return pattern.matcher(regex).matches();
        }

        @Override
        public String toString() {
            return "+" + super.toString();
        }
    }

    public static class Exclude extends Matcher {

        public Exclude(final String patternString) {
            super(patternString);
        }

        @Override
        public boolean test(final String regex) {
            return !pattern.matcher(regex).matches();
        }

        @Override
        public String toString() {
            return "-" + super.toString();
        }
    }

    /**
     * Parses a string representation of an include/exclude filter.
     * <p>
     * The given includeExcludeString is a comma separated list of regexes, each
     * starting with either + or - to indicate include/exclude.
     * <p>
     * For example parsePackages("-java\\..*, -javax\\..*, -sun\\..*,
     * -com\\.sun\\..*") or parse("+com\\.myn\\..*,-com\\.myn\\.excluded\\..*").
     * Note that "-java\\..*" will block "java.foo" but not "javax.foo".
     * <p>
     * See also the more useful {@link FilterBuilder#parsePackages(String)}
     * method.
     */
    public static FilterBuilder parse(String includeExcludeString) {
        List<Predicate<String>> filters = new ArrayList<>();

        if (!Utils.isEmpty(includeExcludeString)) {
            for (String string : Splitter.on(',').split(includeExcludeString)) {
                String trimmed = string.trim();
                char prefix = trimmed.charAt(0);
                String pattern = trimmed.substring(1);

                Predicate<String> filter;
                switch (prefix) {
                    case '+':
                        filter = new Include(pattern);
                        break;
                    case '-':
                        filter = new Exclude(pattern);
                        break;
                    default:
                        throw new ReflectionException("includeExclude should start with either + or -");
                }

                filters.add(filter);
            }

            return new FilterBuilder(filters);
        } else {
            return new FilterBuilder();
        }
    }

    /**
     * Parses a string representation of an include/exclude filter.
     * <p>
     * The given includeExcludeString is a comma separated list of package name
     * segments, each starting with either + or - to indicate include/exclude.
     * <p>
     * For example parsePackages("-java, -javax, -sun, -com.sun") or
     * parse("+com.myn,-com.myn.excluded"). Note that "-java" will block
     * "java.foo" but not "javax.foo".
     * <p>
     * The input strings "-java" and "-java." are equivalent.
     */
    public static FilterBuilder parsePackages(String includeExcludeString) {
        List<Predicate<String>> filters = new ArrayList<>();

        if (!Utils.isEmpty(includeExcludeString)) {
            for (String string : Splitter.on(',').split(includeExcludeString)) {
                String trimmed = string.trim();
                char prefix = trimmed.charAt(0);
                String pattern = trimmed.substring(1);
                if (!pattern.endsWith(".")) {
                    pattern += ".";
                }
                pattern = prefix(pattern);

                Predicate<String> filter;
                switch (prefix) {
                    case '+':
                        filter = new Include(pattern);
                        break;
                    case '-':
                        filter = new Exclude(pattern);
                        break;
                    default:
                        throw new ReflectionException("includeExclude should start with either + or -");
                }

                filters.add(filter);
            }

            return new FilterBuilder(filters);
        } else {
            return new FilterBuilder();
        }
    }
}
