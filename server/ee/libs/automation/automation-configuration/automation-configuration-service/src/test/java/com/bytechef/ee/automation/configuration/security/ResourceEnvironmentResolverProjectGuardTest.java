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
 * <p>
 * {@code PermissionServiceImpl#hasWorkspaceScopeForProject} delegates to {@code hasResourceScope(projectId, "Project",
 * scope)} so that project-keyed checks pick up the visibility precondition. {@code hasResourceScope} has a second
 * behaviour that delegation inherits along the way: after resolving the owning workspace, it looks up a resolver for
 * the resource type and, if one exists and can answer, checks the scope in that specific environment instead of the
 * environment-unaware union of every environment the caller can reach.
 * </p>
 *
 * <p>
 * A {@code Project} has no environment of its own — environments belong to its <em>deployments</em>
 * ({@code ProjectDeployment}), not to the project. The moment something registers a resolver for {@code "Project"},
 * every project-keyed check silently becomes environment-aware for a resource with no environment to answer with.
 * Nothing would fail loudly; the effective permission would just change. This test exists to make that change loud
 * instead of silent.
 * </p>
 *
 * <p>
 * <strong>Coverage.</strong> Resolvers are discovered by scanning this module's classpath for concrete implementations
 * of {@code ResourceEnvironmentResolver} (the same {@code ClassPathScanningCandidateComponentProvider} idiom used by
 * {@code DeferredEvaluationParameterKeysLoader}) rather than by hand-listing the resolvers known today — a hardcoded
 * list would not notice a new one being added, which is the entire risk this guards against. Each discovered class is
 * instantiated via {@code Mockito.mock(clazz, CALLS_REAL_METHODS)}, which bypasses the constructor (and therefore any
 * repository/service dependencies a resolver needs) while still running its real {@code resourceType()} body. The scan
 * only sees classes reachable on THIS module's test classpath: it covers the CE
 * {@code automation-configuration-service} module (already a dependency here, and the module that owns the
 * {@code Project} domain — the module a {@code "Project"} resolver would most plausibly be added to) as well as this EE
 * module itself, but it would NOT catch a resolver contributed from a module this one does not depend on, such as
 * {@code automation-ai-mcp-service}. The first assertion below pins that the scan genuinely finds resolvers on this
 * classpath, so a change that silently breaks discovery (e.g. a filter or base package typo) fails loudly instead of
 * leaving this test vacuously green forever.
 * </p>
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

        // Sanity check on the discovery mechanism itself: if this ever comes back empty (or missing a resolver known
        // to be on this classpath) the "no Project resolver" assertion below would be vacuously true no matter what,
        // which is worse than no guard at all.
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
