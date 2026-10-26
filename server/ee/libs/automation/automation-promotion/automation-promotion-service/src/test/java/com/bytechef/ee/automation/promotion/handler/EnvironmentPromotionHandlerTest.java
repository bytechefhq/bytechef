/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.Environment;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnvironmentPromotionHandlerTest {

    private static final String HANDLER_PACKAGE = "com.bytechef.ee.automation.promotion.handler";
    private static final String CONNECTION_SCOPE_OWNER = "com/bytechef/ee/automation/promotion/connection/" +
        "PromotionConnectionScope";
    private static final String CONNECTION_SCOPE_METHOD = "checkMappedConnectionsBelongToSource";

    private static final Class<?>[] PREVIEW_PARAMETER_TYPES = {
        long.class, Environment.class
    };
    private static final Class<?>[] PROMOTE_PARAMETER_TYPES = {
        long.class, Environment.class, Map.class
    };

    @Test
    void testApiCollectionPromotionHandlerPreviewRequiresProjectDeploymentPush() {
        assertPromotionExpression(
            ApiCollectionPromotionHandler.class, "preview", PREVIEW_PARAMETER_TYPES,
            "hasPermission(@promotionAuthorizer.projectIdOfApiCollection(#sourceId), 'Project', 'DEPLOYMENT_PUSH')");
    }

    @Test
    void testApiCollectionPromotionHandlerPromoteRequiresProjectDeploymentPush() {
        assertPromotionExpression(
            ApiCollectionPromotionHandler.class, "promote", PROMOTE_PARAMETER_TYPES,
            "hasPermission(@promotionAuthorizer.projectIdOfApiCollection(#sourceId), 'Project', 'DEPLOYMENT_PUSH')");
    }

    @Test
    void testMcpServerPromotionHandlerPreviewRequiresWorkspaceMcpCreate() {
        assertPromotionExpression(
            McpServerPromotionHandler.class, "preview", PREVIEW_PARAMETER_TYPES,
            "hasPermission(@promotionAuthorizer.workspaceIdOfMcpServer(#sourceId), 'Workspace', 'MCP_CREATE')");
    }

    @Test
    void testMcpServerPromotionHandlerPromoteRequiresWorkspaceMcpCreate() {
        assertPromotionExpression(
            McpServerPromotionHandler.class, "promote", PROMOTE_PARAMETER_TYPES,
            "hasPermission(@promotionAuthorizer.workspaceIdOfMcpServer(#sourceId), 'Workspace', 'MCP_CREATE')");
    }

    @Test
    void testProjectDeploymentPromotionHandlerPreviewRequiresProjectDeploymentPush() {
        assertPromotionExpression(
            ProjectDeploymentPromotionHandler.class, "preview", PREVIEW_PARAMETER_TYPES,
            "hasPermission(@promotionAuthorizer.projectIdOfProjectDeployment(#sourceId), 'Project', " +
                "'DEPLOYMENT_PUSH')");
    }

    @Test
    void testProjectDeploymentPromotionHandlerPromoteRequiresProjectDeploymentPush() {
        assertPromotionExpression(
            ProjectDeploymentPromotionHandler.class, "promote", PROMOTE_PARAMETER_TYPES,
            "hasPermission(@promotionAuthorizer.projectIdOfProjectDeployment(#sourceId), 'Project', " +
                "'DEPLOYMENT_PUSH')");
    }

    @Test
    void testEveryPromotionHandlerPromoteCallsConnectionScopeCheckBeforeMappingSourceConnections() {
        Set<Class<?>> handlerClasses = discoverEnvironmentPromotionHandlerClasses();

        assertThat(handlerClasses)
            .as("classes assignable to %s discovered under %s", EnvironmentPromotionHandler.class.getSimpleName(),
                HANDLER_PACKAGE)
            .contains(
                ApiCollectionPromotionHandler.class, McpServerPromotionHandler.class,
                ProjectDeploymentPromotionHandler.class);

        for (Class<?> handlerClass : handlerClasses) {
            assertThat(promoteCallsConnectionScopeCheck(handlerClass))
                .as(
                    "%s#promote calls PromotionConnectionScope.checkMappedConnectionsBelongToSource before " +
                        "ConnectionEnvironmentMapper.validate",
                    handlerClass.getSimpleName())
                .isTrue();
        }
    }

    private static void assertPromotionExpression(
        Class<?> handlerClass, String methodName, Class<?>[] parameterTypes, String expression) {

        Method[] declaredMethods = handlerClass.getDeclaredMethods();

        Method match = Arrays.stream(declaredMethods)
            .filter(candidate -> candidate.getName()
                .equals(methodName))
            .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), parameterTypes))
            .findFirst()
            .orElse(null);

        assertThat(match)
            .as("method %s%s on %s", methodName, Arrays.toString(parameterTypes), handlerClass.getSimpleName())
            .isNotNull();

        PreAuthorize preAuthorize = match.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s#%s", handlerClass.getSimpleName(), methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }

    private static Set<Class<?>> discoverEnvironmentPromotionHandlerClasses() {
        MockEnvironment environment = new MockEnvironment();

        environment.setProperty("bytechef.edition", "ee");

        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false, environment);

        scanner.addIncludeFilter(new AssignableTypeFilter(EnvironmentPromotionHandler.class));

        Set<Class<?>> handlerClasses = new HashSet<>();

        for (BeanDefinition beanDefinition : scanner.findCandidateComponents(HANDLER_PACKAGE)) {
            String beanClassName = beanDefinition.getBeanClassName();

            try {
                handlerClasses.add(Class.forName(beanClassName));
            } catch (ClassNotFoundException classNotFoundException) {
                throw new IllegalStateException(
                    "Discovered candidate class %s could not be loaded".formatted(beanClassName),
                    classNotFoundException);
            }
        }

        return handlerClasses;
    }

    private static boolean promoteCallsConnectionScopeCheck(Class<?> handlerClass) {
        String resourceName = handlerClass.getName()
            .replace('.', '/') + ".class";

        try (InputStream classBytes = handlerClass.getClassLoader()
            .getResourceAsStream(resourceName)) {

            assertThat(classBytes)
                .as("class bytes for %s", handlerClass.getName())
                .isNotNull();

            ClassReader classReader = new ClassReader(classBytes);
            PromoteMethodCallVisitor visitor = new PromoteMethodCallVisitor();

            classReader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            return visitor.callsConnectionScopeCheck;
        } catch (IOException ioException) {
            throw new IllegalStateException(
                "Could not read class bytes for %s".formatted(handlerClass.getName()), ioException);
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

            if (!"promote".equals(name) || (access & Opcodes.ACC_PUBLIC) == 0) {
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
