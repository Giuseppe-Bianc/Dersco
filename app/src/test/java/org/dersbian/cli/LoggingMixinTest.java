package org.dersbian.cli;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link LoggingMixin}.
 *
 * <p>Verifies that {@code applyLogLevel()} reconfigures the root logger according to the mixin's
 * own options. We attach a {@link ListAppender} to observe the root level without coupling to
 * logback internals beyond the public API.
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
class LoggingMixinTest {

    private Logger root;
    private ListAppender<ILoggingEvent> appender;
    private Level originalRootLevel;

    @BeforeEach
    void attachAppender() {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        originalRootLevel = root.getLevel();
        appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        root.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        root.detachAppender(appender);
        appender.stop();
        // Reset root level so a misbehaving test cannot leak into the next one.
        root.setLevel(originalRootLevel);
    }

    @Test
    void noFlagsResolvesToWarn() {
        final LoggingMixin mixin = new LoggingMixin();

        mixin.applyLogLevel();

        assertThat(root.getLevel()).isEqualTo(Level.WARN);
    }

    @Test
    void singleVerboseFlagResolvesToInfo() {
        final LoggingMixin mixin = mixinWithVerbose(1);

        mixin.applyLogLevel();

        assertThat(root.getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void twoVerboseFlagsResolveToDebug() {
        final LoggingMixin mixin = mixinWithVerbose(2);

        mixin.applyLogLevel();

        assertThat(root.getLevel()).isEqualTo(Level.DEBUG);
    }

    @Test
    void threeOrMoreVerboseFlagsResolveToTrace() {
        final LoggingMixin mixin = mixinWithVerbose(3);

        mixin.applyLogLevel();

        assertThat(root.getLevel()).isEqualTo(Level.TRACE);
    }

    @Test
    void quietOverridesAnyVerboseFlag() {
        final LoggingMixin mixin = mixinWithVerbose(5);
        setQuiet(mixin, true);

        mixin.applyLogLevel();

        assertThat(root.getLevel()).isEqualTo(Level.ERROR);
    }

    /**
     * Reflectively assigns {@code verbose.length == count}. The field is private and written by
     * picocli at parse time; we are testing the resolved behavior, not the parsing.
     */
    private static LoggingMixin mixinWithVerbose(final int count) {
        final LoggingMixin mixin = new LoggingMixin();
        setField(mixin, "verbosity", new boolean[count]);
        return mixin;
    }

    private static void setQuiet(final LoggingMixin mixin, final boolean value) {
        setField(mixin, "quiet", value);
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void setField(final Object target, final String name, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new LinkageError("Failed to set " + name + " via reflection", e);
        }
    }
}
