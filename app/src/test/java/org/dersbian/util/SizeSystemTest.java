package org.dersbian.util;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.TooManyMethods",
    "PMD.UnitTestContainsTooManyAsserts"
})
class SizeSystemTest {

    private static final List<String> VALID_PREFIXES = List.of("", "K", "M", "G", "T", "P");

    // -----------------------------------------------------------------------
    // Constructor – happy-path field exposure
    // -----------------------------------------------------------------------

    @Test
    void constructorExposesName() {
        final SizeSystem sys = new SizeSystem("SI", 1000.0, VALID_PREFIXES);

        Assertions.assertEquals("SI", sys.name());
    }

    @Test
    void constructorExposesBase() {
        final SizeSystem sys = new SizeSystem("SI", 1000.0, VALID_PREFIXES);

        Assertions.assertEquals(1000.0, sys.base());
    }

    @Test
    void constructorExposesPrefixes() {
        final SizeSystem sys = new SizeSystem("SI", 1000.0, VALID_PREFIXES);

        Assertions.assertEquals(VALID_PREFIXES, sys.prefixes());
    }

    // -----------------------------------------------------------------------
    // Defensive copy of prefixes list
    // -----------------------------------------------------------------------

    @Test
    void prefixesListIsDefensivelyCopied() {
        final List<String> mutable = new ArrayList<>(VALID_PREFIXES);
        final SizeSystem sys = new SizeSystem("SI", 1000.0, mutable);

        mutable.clear();

        Assertions.assertEquals(VALID_PREFIXES, sys.prefixes());
    }

    @Test
    void prefixesListIsNotSameReferenceAsMutable() {
        final List<String> mutable = new ArrayList<>(VALID_PREFIXES);
        final SizeSystem sys = new SizeSystem("SI", 1000.0, mutable);

        Assertions.assertNotSame(mutable, sys.prefixes());
    }

    @Test
    void prefixesIsImmutableForTheCaller() {
        final SizeSystem sys = new SizeSystem("SI", 1000.0, VALID_PREFIXES);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> sys.prefixes().add("Z"));
    }

    // -----------------------------------------------------------------------
    // Null name validation
    // -----------------------------------------------------------------------

    @Test
    void nullNameIsRejectedWithNonNullMessage() {
        final NullPointerException exception =
                Assertions.assertThrows(
                        NullPointerException.class,
                        () -> new SizeSystem(null, 1000.0, VALID_PREFIXES));

        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void nullNameIsRejectedWithMessageContainingName() {
        final NullPointerException exception =
                Assertions.assertThrows(
                        NullPointerException.class,
                        () -> new SizeSystem(null, 1000.0, VALID_PREFIXES));

        Assertions.assertTrue(exception.getMessage().contains("name"));
    }

    // -----------------------------------------------------------------------
    // Null prefixes validation
    // -----------------------------------------------------------------------

    @Test
    void nullPrefixesIsRejectedWithNonNullMessage() {
        final NullPointerException exception =
                Assertions.assertThrows(
                        NullPointerException.class, () -> new SizeSystem("SI", 1000.0, null));

        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void nullPrefixesIsRejectedWithMessageContainingPrefixes() {
        final NullPointerException exception =
                Assertions.assertThrows(
                        NullPointerException.class, () -> new SizeSystem("SI", 1000.0, null));

        Assertions.assertTrue(exception.getMessage().contains("prefixes"));
    }

    // -----------------------------------------------------------------------
    // Prefix-count validation
    // -----------------------------------------------------------------------

    @Test
    void tooShortPrefixCountIsRejected() {
        final List<String> tooShort = List.of("B", "KB", "MB", "GB", "TB");

        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> new SizeSystem("SI", 1000.0, tooShort));

        Assertions.assertTrue(exception.getMessage().contains("6"));
    }

    @Test
    void tooLongPrefixCountIsRejected() {
        final List<String> tooLong = new ArrayList<>(VALID_PREFIXES);
        tooLong.add("EB");

        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> new SizeSystem("SI", 1000.0, tooLong));

        Assertions.assertTrue(exception.getMessage().contains("6"));
    }

    // -----------------------------------------------------------------------
    // Base value validation
    // -----------------------------------------------------------------------

    @Test
    void baseOfZeroIsRejected() {
        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> new SizeSystem("X", 0.0, VALID_PREFIXES));

        Assertions.assertTrue(exception.getMessage().contains("base"));
    }

    @Test
    void negativeBaseIsRejected() {
        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> new SizeSystem("X", -2.0, VALID_PREFIXES));

        Assertions.assertTrue(exception.getMessage().contains("base"));
    }

    @Test
    void baseEqualToOneIsAccepted() {
        final SizeSystem sys = new SizeSystem("Linear", 1.0, VALID_PREFIXES);

        Assertions.assertEquals(1.0, sys.base());
    }
}
