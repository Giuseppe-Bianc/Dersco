package org.dersbian.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ManifestVersionProvider}.
 *
 * <p>The provider reads the {@code Implementation-Version} attribute of the JAR manifest. Under
 * Gradle's test classpath, the manifest is not packaged with the test JAR, so the attribute is
 * {@code null} and the provider returns the fallback string. We assert that behavior here, plus the
 * exact shape of the fallback.
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
class ManifestVersionProviderTest {

    @Test
    void versionHasExactlyOneLine() {
        final String[] version = new ManifestVersionProvider().getVersion();

        assertThat(version).hasSize(1);
    }

    @Test
    void versionStartsWithDerscoPrefix() {
        final String[] version = new ManifestVersionProvider().getVersion();

        assertThat(version[0]).startsWith("dersco ");
    }

    @Test
    void versionFallsBackToDevelopmentWhenManifestAttributeMissing() {
        final String[] version = new ManifestVersionProvider().getVersion();

        assertThat(version[0]).isEqualTo("dersco development");
    }
}
