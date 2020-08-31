package com.phyzicsz.rocket.reflection;

import com.phyzicsz.rocket.reflection.util.FilterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 * Test filtering
 */
public class FilterBuilderTest {

    @Test
    public void test_include() {
        FilterBuilder filter = new FilterBuilder().include("com\\.phyzicsz.rocket.reflection.*");
        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isTrue();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isTrue();
        assertThat(filter.test("org.foobar.Reflections")).isFalse();
    }

    @Test
    public void test_includePackage() {
        FilterBuilder filter = new FilterBuilder().includePackage("com.phyzicsz.rocket.reflection");
        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isTrue();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isTrue();
        assertThat(filter.test("org.foobar.Reflections")).isFalse();
    }

    @Test
    public void test_includePackageMultiple() {
        FilterBuilder filter = new FilterBuilder().includePackage("com.phyzicsz.rocket.reflection", "org.foo");
        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isTrue();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isTrue();
        assertThat(filter.test("org.foo.Reflections")).isTrue();
        assertThat(filter.test("org.foo.bar.Reflections")).isTrue();
        assertThat(filter.test("org.bar.Reflections")).isFalse();
    }

    @Test
    public void test_includePackagebyClass() {
        FilterBuilder filter = new FilterBuilder().includePackage(RocketReflection.class);
        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isTrue();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isTrue();
        assertThat(filter.test("org.foobar.Reflections")).isFalse();
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_exclude() {
        FilterBuilder filter = new FilterBuilder().exclude("com\\.phyzicsz.rocket.reflection.*");
        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isFalse();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foobar.Reflections")).isTrue();
    }

    @Test
    public void test_excludePackage() {
        FilterBuilder filter = new FilterBuilder().excludePackage("com.phyzicsz.rocket.reflection");
        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isFalse();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foobar.Reflections")).isTrue();
    }

    @Test
    public void test_excludePackageMultiple() {
        FilterBuilder filter = new FilterBuilder().excludePackage("com.phyzicsz.rocket.reflection", "org.foo");
        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isFalse();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foo.bar.Reflections")).isFalse();
        assertThat(filter.test("org.bar.Reflections")).isTrue();
    }

    @Test
    public void test_excludePackageByClass() {
        FilterBuilder filter = new FilterBuilder().excludePackage(RocketReflection.class);
        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isFalse();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foobar.Reflections")).isTrue();
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_include() {
        FilterBuilder filter = FilterBuilder.parse("+com.phyzicsz.rocket.reflection.*");
        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isTrue();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isTrue();
        assertThat(filter.test("org.foobar.Reflections")).isFalse();
    }

    @Test
    public void test_parse_include_notRegex() {
        FilterBuilder filter = FilterBuilder.parse("+com.phyzicsz.rocket.reflection.reflection");

        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isFalse();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foobar.Reflections")).isFalse();
    }

    @Test
    public void test_parse_exclude() {
        FilterBuilder filter = FilterBuilder.parse("-com.phyzicsz.rocket.reflection.*");

        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isFalse();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foobar.Reflections")).isTrue();
    }

    @Test
    public void test_parse_exclude_notRegex() {
        FilterBuilder filter = FilterBuilder.parse("-com.phyzicsz.rocket.reflection");

        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isTrue();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isTrue();
        assertThat(filter.test("org.foobar.Reflections")).isTrue();
    }

    @Test
    public void test_parse_include_exclude() {
        FilterBuilder filter = FilterBuilder.parse("+com.phyzicsz.rocket.reflection.*, -com.phyzicsz.rocket.reflection.foo.*");

        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isTrue();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foobar.Reflections")).isFalse();
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parsePackages_include() {
        FilterBuilder filter = FilterBuilder.parsePackages("+com.phyzicsz.rocket.reflection");

        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isTrue();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isTrue();
        assertThat(filter.test("org.foobar.Reflections")).isFalse();
    }

    @Test
    public void test_parsePackages_include_trailingDot() {
        FilterBuilder filter = FilterBuilder.parsePackages("+com.phyzicsz.rocket.reflection.");

        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isTrue();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isTrue();
        assertThat(filter.test("org.foobar.Reflections")).isFalse();
    }

    @Test
    public void test_parsePackages_exclude() {
        FilterBuilder filter = FilterBuilder.parsePackages("-com.phyzicsz.rocket.reflection");

        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isFalse();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foobar.Reflections")).isTrue();
    }

    @Test
    public void test_parsePackages_exclude_trailingDot() {
        FilterBuilder filter = FilterBuilder.parsePackages("-com.phyzicsz.rocket.reflection.");

        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isFalse();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foobar.Reflections")).isTrue();
    }

    @Test
    public void test_parsePackages_include_exclude() {
        FilterBuilder filter = FilterBuilder.parsePackages("+com.phyzicsz.rocket.reflection, -com.phyzicsz.rocket.reflection.foo");

        assertThat(filter.test("com.phyzicsz.rocket.reflection.Reflections")).isTrue();
        assertThat(filter.test("com.phyzicsz.rocket.reflection.foo.Reflections")).isFalse();
        assertThat(filter.test("org.foobar.Reflections")).isFalse();
    }

}
