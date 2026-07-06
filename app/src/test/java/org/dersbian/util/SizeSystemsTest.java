package org.dersbian.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class SizeSystemsTest {

    @Test
    void siSystemIsDecimalAndHasSixPrefixes() {
        final SizeSystem siSystem = SizeSystems.SI_SYSTEM;

        Assertions.assertAll(
                () -> Assertions.assertEquals("SI", siSystem.name()),
                () -> Assertions.assertEquals(1000.0, siSystem.base()),
                () ->
                        Assertions.assertEquals(
                                List.of("B", "KB", "MB", "GB", "TB", "PB"), siSystem.prefixes()));
    }

    @Test
    void iecSystemIsBinaryAndHasSixPrefixes() {
        final SizeSystem iecSystem = SizeSystems.IEC;

        Assertions.assertAll(
                () -> Assertions.assertEquals("IEC", iecSystem.name()),
                () -> Assertions.assertEquals(1024.0, iecSystem.base()),
                () ->
                        Assertions.assertEquals(
                                List.of("B", "KiB", "MiB", "GiB", "TiB", "PiB"),
                                iecSystem.prefixes()));
    }

    @Test
    void builtInPrefixesAreImmutable() {
        final SizeSystem siSystem = SizeSystems.SI_SYSTEM;
        final SizeSystem iecSystem = SizeSystems.IEC;

        Assertions.assertAll(
                () ->
                        Assertions.assertThrows(
                                UnsupportedOperationException.class,
                                () -> siSystem.prefixes().add("Z")),
                () ->
                        Assertions.assertThrows(
                                UnsupportedOperationException.class,
                                () -> iecSystem.prefixes().add("Z")));
    }

    @Test
    void constructorIsPrivate() throws NoSuchMethodException {
        final Constructor<SizeSystems> constructor = SizeSystems.class.getDeclaredConstructor();

        Assertions.assertAll(
                () -> Assertions.assertEquals(0, constructor.getParameterCount()),
                () ->
                        Assertions.assertTrue(
                                Modifier.isPrivate(constructor.getModifiers()),
                                "constructor must be private"));
    }

    @Test
    void instancesAreReferentiallyStable() {
        Assertions.assertAll(
                () -> Assertions.assertSame(SizeSystems.SI_SYSTEM, SizeSystems.SI_SYSTEM),
                () -> Assertions.assertSame(SizeSystems.IEC, SizeSystems.IEC));
    }
}
