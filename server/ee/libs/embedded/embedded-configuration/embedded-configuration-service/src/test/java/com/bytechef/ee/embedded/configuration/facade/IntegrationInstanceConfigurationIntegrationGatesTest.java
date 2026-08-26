/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.security.EmbeddedIntegrationAuthorization;
import com.bytechef.platform.configuration.domain.Environment;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Pins the authorization on the two integration reads that the embedded connected-user path depends on.
 *
 * <p>
 * A {@code @PreAuthorize} expression is a string resolved at runtime, so a renamed bean or predicate method fails as a
 * 500 in production rather than at compile time. These tests tie the string to the real class.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationInstanceConfigurationIntegrationGatesTest {

    @Test
    void testByIdReadIsGatedOnCallerOwnership() throws NoSuchMethodException {
        Method method = IntegrationInstanceConfigurationFacadeImpl.class.getMethod(
            "getIntegrationInstanceConfigurationIntegration", long.class, boolean.class, Environment.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value())
            .isEqualTo("@embeddedIntegrationAuthorization.canAccessIntegration(#integrationId, #environment)");

        // The vestigial admin gate must not come back: no admin surface calls this, and restoring it 403s the
        // connected-user path the moment skip mode stops widening.
        assertThat(preAuthorize.value()).doesNotContain("isTenantAdmin");
    }

    @Test
    void testByIdGateReferencesARealBeanAndMethod() throws NoSuchMethodException {
        Component component = EmbeddedIntegrationAuthorization.class.getAnnotation(Component.class);

        assertThat(component).isNotNull();
        assertThat(component.value()).isEqualTo("embeddedIntegrationAuthorization");

        Method predicate = EmbeddedIntegrationAuthorization.class.getMethod(
            "canAccessIntegration", long.class, Environment.class);

        assertThat(predicate.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    void testByIdGateSpelArgumentNamesMatchTheMethodParameters() throws NoSuchMethodException {
        // #integrationId / #environment are resolved by name at runtime against the method's real parameter names.
        // Renaming a parameter silently makes the expression evaluate against null rather than failing loudly, so bind
        // the two together here. Requires -parameters, which the build sets; assert that rather than pass vacuously.
        Method method = IntegrationInstanceConfigurationFacadeImpl.class.getMethod(
            "getIntegrationInstanceConfigurationIntegration", long.class, boolean.class, Environment.class);

        assertThat(method.getParameters()[0].isNamePresent())
            .as("compiled without -parameters, so SpEL argument names cannot be verified")
            .isTrue();

        assertThat(method.getParameters())
            .extracting(Parameter::getName)
            .contains("integrationId", "environment");
    }

    @Test
    void testTheListReadStillHasExactlyOneCaller() throws IOException {
        // The weak gate on the list read is only safe because its single caller filters per row. A second caller that
        // forgot to filter would be a silent hole, and nothing else in the build would notice -- so count them.
        // Resolved from the compiled class location, not the process CWD, so this behaves the same under Gradle and
        // under an IDE runner (whose working directory is typically the project root).
        Path sourceRoot = moduleSourceRoot();

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            List<String> callers = paths.filter(path -> path.toString()
                .endsWith(".java"))
                .filter(path -> {
                    try {
                        String source = Files.readString(path);

                        // Exclude the declaring class: it self-invokes from the by-id read, which bypasses the proxy.
                        return source.contains("getIntegrationInstanceConfigurationIntegrations(")
                            && !path.endsWith("IntegrationInstanceConfigurationFacadeImpl.java");
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                })
                .map(path -> String.valueOf(path.getFileName()))
                .toList();

            assertThat(callers)
                .as(
                    "a new caller of the unguarded list read must apply its own per-row filtering; see the method's " +
                        "Javadoc")
                .containsExactly("ConnectedUserIntegrationFacadeImpl.java");
        }
    }

    /**
     * The module's {@code src/main/java}, located by walking up from the compiled classes directory
     * ({@code <module>/build/classes/java/test}) rather than trusting the process working directory.
     */
    private static Path moduleSourceRoot() {
        Path path;

        try {
            path = Path.of(
                IntegrationInstanceConfigurationIntegrationGatesTest.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(exception);
        }

        while (path != null && !Files.isDirectory(path.resolve("src/main/java"))) {
            path = path.getParent();
        }

        if (path == null) {
            throw new IllegalStateException("Could not locate the module's src/main/java");
        }

        return path.resolve("src/main/java");
    }

    @Test
    void testListReadIsDeliberatelyOnlyAuthenticated() throws NoSuchMethodException {
        // Entry gating cannot express per-element ownership here (the method takes no resource id). Its sole caller,
        // ConnectedUserIntegrationFacadeImpl#getConnectedUserIntegrations, filters each row through
        // isIntegrationVisible -- covered by ConnectedUserIntegrationFacadeFilterTest, not duplicated here.
        Method method = IntegrationInstanceConfigurationFacadeImpl.class.getMethod(
            "getIntegrationInstanceConfigurationIntegrations", boolean.class, Environment.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("isAuthenticated()");
    }

    @Test
    void testTheListReadStillHasExactlyOneCallerWorthOfArguments() throws NoSuchMethodException {
        // Guards the premise of the weak gate: the method carries no resource id, which is WHY entry gating cannot
        // authorize it. If an id argument is ever added, entry gating becomes expressible and this should be revisited.
        Method method = IntegrationInstanceConfigurationFacadeImpl.class.getMethod(
            "getIntegrationInstanceConfigurationIntegrations", boolean.class, Environment.class);

        assertThat(Arrays.stream(method.getParameterTypes()))
            .noneMatch(parameterType -> parameterType == long.class || parameterType == Long.class);
    }
}
