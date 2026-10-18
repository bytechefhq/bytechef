/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * Pins that no {@link ResourceEnvironmentResolver} claims the {@code "Project"} resource type.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ResourceEnvironmentResolverProjectGuardTest {

    private static final String BASE_PACKAGE = "com.bytechef";
    private static final String PROJECT = "Project";

    @Test
    void testDiscoveryFindsTheResolversKnownToExistOnThisClasspath() {
        List<String> resourceTypes = discoverResourceTypes();

        assertThat(resourceTypes).contains("Connection", "ProjectDeployment");
    }

    @Test
    void testNoResourceEnvironmentResolverClaimsProject() {
        List<String> resourceTypes = discoverResourceTypes();

        assertThat(resourceTypes)
            .as(
                "a Project has no environment of its own -- environments belong to its deployments -- so a resolver "
                    + "claiming \"Project\" here would silently make every project-keyed permission check "
                    + "environment-aware for a resource that has no environment to answer with")
            .doesNotContain(PROJECT);
    }

    private static List<String> discoverResourceTypes() {
        List<String> resourceTypes = new ArrayList<>();

        for (Class<?> resolverClass : discoverResourceEnvironmentResolverClasses()) {
            ResourceEnvironmentResolver resolver =
                (ResourceEnvironmentResolver) mock(resolverClass, CALLS_REAL_METHODS);

            resourceTypes.add(resolver.resourceType());
        }

        return resourceTypes;
    }

    private static List<Class<?>> discoverResourceEnvironmentResolverClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AssignableTypeFilter(ResourceEnvironmentResolver.class));

        List<Class<?>> resolverClasses = new ArrayList<>();

        for (BeanDefinition beanDefinition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            String beanClassName = beanDefinition.getBeanClassName();

            if (beanClassName == null) {
                continue;
            }

            try {
                resolverClasses.add(
                    Class.forName(beanClassName, false, Thread.currentThread()
                        .getContextClassLoader()));
            } catch (ClassNotFoundException classNotFoundException) {
                throw new IllegalStateException(
                    "Unable to load ResourceEnvironmentResolver class: " + beanClassName, classNotFoundException);
            }
        }

        return resolverClasses;
    }
}
