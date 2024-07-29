package com.xkcoding.loader.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.loader.util.SystemPropertyUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SystemPropertyUtilsTests {

    @BeforeClass
    public static void init() {
        System.setProperty("foo","bar");
    }

    @AfterClass
    public static void close() {
        System.clearProperty("foo");
    }

    @Test
    public void testVanillaPlaceholder() {
        assertEquals(SystemPropertyUtils.resolvePlaceholders("${foo}"), "bar");
    }

}
