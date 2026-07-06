package org.dersbian.cli;

import picocli.CommandLine.IVersionProvider;

/**
 * Reads the application version from the {@code Implementation-Version} attribute of the JAR
 * manifest (populated by Gradle at build time), avoiding version duplication between {@code
 * build.gradle.kts} and source code.
 */
@SuppressWarnings({"PMD.CommentSize", "PMD.AtLeastOneConstructor"})
public final class ManifestVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
        final String version = getClass().getPackage().getImplementationVersion();
        return new String[] {"dersco " + (version != null ? version : "development")};
    }
}
