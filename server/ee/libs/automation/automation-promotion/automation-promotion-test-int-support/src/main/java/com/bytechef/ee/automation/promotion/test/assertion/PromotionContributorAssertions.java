/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.test.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class PromotionContributorAssertions {

    private static final String CONNECTION_SCOPE_METHOD = "checkMappedConnectionsBelongToSource";
    private static final String CONNECTION_SCOPE_OWNER =
        "com/bytechef/ee/automation/configuration/promotion/connection/PromotionConnectionScope";
    private static final String PROMOTE_METHOD = "promote";

    private PromotionContributorAssertions() {
    }

    public static void assertPreAuthorize(
        Class<?> contributorClass, String methodName, Class<?>[] parameterTypes, String expression) {

        Method match = null;

        for (Method declaredMethod : contributorClass.getDeclaredMethods()) {
            String declaredMethodName = declaredMethod.getName();

            if (declaredMethodName.equals(methodName) &&
                Arrays.equals(declaredMethod.getParameterTypes(), parameterTypes)) {

                match = declaredMethod;

                break;
            }
        }

        assertThat(match)
            .as("method %s%s on %s", methodName, Arrays.toString(parameterTypes), contributorClass.getSimpleName())
            .isNotNull();

        PreAuthorize preAuthorize = match.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s#%s", contributorClass.getSimpleName(), methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }

    public static Set<Class<?>> assertEveryContributorCallsConnectionScopeCheck(String... basePackages) {
        Set<Class<?>> contributorClasses = discoverEnvironmentPromotionContributorClasses(basePackages);

        assertThat(contributorClasses)
            .as("classes assignable to %s discovered under %s", EnvironmentPromotionContributor.class.getSimpleName(),
                List.of(basePackages))
            .isNotEmpty();

        for (Class<?> contributorClass : contributorClasses) {
            assertThat(promoteCallsConnectionScopeCheck(contributorClass))
                .as(
                    "%s#promote(long, Environment, Environment, Map) contains a call to " +
                        "PromotionConnectionScope.checkMappedConnectionsBelongToSource",
                    contributorClass.getSimpleName())
                .isTrue();
        }

        return contributorClasses;
    }

    public static Set<Class<?>> discoverEnvironmentPromotionContributorClasses(String... basePackages) {
        StandardEnvironment environment = new StandardEnvironment();

        MutablePropertySources propertySources = environment.getPropertySources();

        propertySources.addFirst(new MapPropertySource("promotionContributorAssertions", Map.of(
            "bytechef.edition", "ee")));

        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false, environment);

        scanner.addIncludeFilter(new AssignableTypeFilter(EnvironmentPromotionContributor.class));

        Set<Class<?>> contributorClasses = new HashSet<>();

        for (String basePackage : basePackages) {
            for (BeanDefinition beanDefinition : scanner.findCandidateComponents(basePackage)) {
                String beanClassName = beanDefinition.getBeanClassName();

                try {
                    contributorClasses.add(Class.forName(beanClassName));
                } catch (ClassNotFoundException classNotFoundException) {
                    throw new IllegalStateException(
                        "Discovered candidate class %s could not be loaded".formatted(beanClassName),
                        classNotFoundException);
                }
            }
        }

        return contributorClasses;
    }

    private static boolean promoteCallsConnectionScopeCheck(Class<?> contributorClass) {
        String contributorClassName = contributorClass.getName();

        String resourceName = contributorClassName.replace('.', '/') + ".class";

        ClassLoader classLoader = contributorClass.getClassLoader();

        try (InputStream classBytes = classLoader.getResourceAsStream(resourceName)) {
            assertThat(classBytes)
                .as("class bytes for %s", contributorClassName)
                .isNotNull();

            ClassReader classReader = new ClassReader(classBytes);
            PromoteMethodCallVisitor promoteMethodCallVisitor = new PromoteMethodCallVisitor();

            classReader.accept(promoteMethodCallVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            return promoteMethodCallVisitor.callsConnectionScopeCheck;
        } catch (IOException ioException) {
            throw new IllegalStateException(
                "Could not read class bytes for %s".formatted(contributorClassName), ioException);
        }
    }

    private static final class PromoteMethodCallVisitor extends ClassVisitor {

        private boolean callsConnectionScopeCheck;

        private PromoteMethodCallVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {

            if (!PROMOTE_METHOD.equals(name) || (access & Opcodes.ACC_PUBLIC) == 0) {
                return null;
            }

            return new MethodVisitor(Opcodes.ASM9) {

                @Override
                public void visitMethodInsn(
                    int opcode, String owner, String methodName, String methodDescriptor, boolean isInterface) {

                    if (CONNECTION_SCOPE_OWNER.equals(owner) && CONNECTION_SCOPE_METHOD.equals(methodName)) {
                        callsConnectionScopeCheck = true;
                    }
                }
            };
        }
    }
}
