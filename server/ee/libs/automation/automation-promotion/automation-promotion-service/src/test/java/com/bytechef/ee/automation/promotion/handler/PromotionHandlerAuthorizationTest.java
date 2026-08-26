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
 * Pins the security surface every {@link EnvironmentPromotionHandler} implementation must carry.
 *
 * <p>
 * Two independent things are pinned here, and neither substitutes for the other:
 * </p>
 * <ul>
 * <li>the exact {@code @PreAuthorize} expression on {@code preview}/{@code promote}, resolved through the separate
 * {@code promotionAuthorizer} bean per ruling R5 (never a handler calling itself through its own security proxy);</li>
 * <li>that {@code promote} calls
 * {@link com.bytechef.ee.automation.promotion.connection.PromotionConnectionScope#checkMappedConnectionsBelongToSource},
 * the mandatory companion to {@code ConnectionEnvironmentMapper#validate} that stops a caller smuggling a source
 * connection id it does not own. That check is {@code public static}, so nothing forces a handler to call it and an
 * omission is silent — no compile error, no failing test elsewhere.</li>
 * </ul>
 *
 * <p>
 * The connection-scope pin discovers implementors by scanning the classpath for concrete classes assignable to
 * {@link EnvironmentPromotionHandler}, rather than naming the four known handler classes, so it also covers a handler
 * added later without needing this test edited. Detection reads the compiled {@code promote} method's bytecode for a
 * call to {@code PromotionConnectionScope.checkMappedConnectionsBelongToSource} — a source-text grep would also catch a
 * call inside a comment, and a behavioural per-handler test (as Tasks 9-11 and 21 already carry) proves only that
 * today's four handlers behave correctly, not that a fifth one will be caught if it forgets the call.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PromotionHandlerAuthorizationTest {

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
    void testA2aServerPromotionHandlerPreviewRequiresAdmin() {
        assertPromotionExpression(
            A2aServerPromotionHandler.class, "preview", PREVIEW_PARAMETER_TYPES, "hasAuthority('ROLE_ADMIN')");
    }

    @Test
    void testA2aServerPromotionHandlerPromoteRequiresAdmin() {
        assertPromotionExpression(
            A2aServerPromotionHandler.class, "promote", PROMOTE_PARAMETER_TYPES, "hasAuthority('ROLE_ADMIN')");
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
                A2aServerPromotionHandler.class, ProjectDeploymentPromotionHandler.class);

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

    /**
     * Finds every concrete class assignable to {@link EnvironmentPromotionHandler} under {@link #HANDLER_PACKAGE},
     * regardless of whether it is annotated {@code @Component} — {@code ApiCollectionPromotionHandler} carries a bare
     * {@code @Component} while the other three name their bean, so filtering on the annotation's value would miss one
     * of today's four handlers, let alone an unnamed one added later.
     *
     * <p>
     * Every handler also carries {@code @ConditionalOnEEVersion}, i.e. {@code @ConditionalOnProperty(bytechef.edition
     * = ee)}. {@code ClassPathScanningCandidateComponentProvider} evaluates {@code @Conditional} metadata against
     * whatever {@link org.springframework.core.env.Environment} it is bound to, and a plain {@code StandardEnvironment}
     * has no such property — every handler would silently fail that check and vanish from the scan, exactly the kind of
     * quiet miss this pin exists to avoid. A {@link MockEnvironment} carrying the property keeps the scan a pure
     * classpath/type-hierarchy query rather than a real bean-registration decision.
     * </p>
     */
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

    /**
     * Reads the compiled {@code promote} method's bytecode for a call to
     * {@link com.bytechef.ee.automation.promotion.connection.PromotionConnectionScope#checkMappedConnectionsBelongToSource}.
     * A bytecode check (rather than a behavioural per-handler test, which Tasks 9-11 and 21 already carry) is the
     * mechanism that keeps working against a handler class nobody has written yet: it inspects whatever {@code promote}
     * method the discovery pass above finds, with no per-class wiring to forget.
     */
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
