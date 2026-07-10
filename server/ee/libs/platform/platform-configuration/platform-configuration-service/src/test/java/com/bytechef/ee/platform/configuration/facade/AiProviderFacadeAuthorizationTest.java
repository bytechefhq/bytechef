/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiProviderFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";

    @Test
    void testDeleteAiProviderRequiresAdmin() {
        assertAdminOnly("deleteAiProvider");
    }

    @Test
    void testGetAiProvidersRequiresAdmin() {
        assertAdminOnly("getAiProviders");
    }

    @Test
    void testGetAiDefaultChatModelApiKeyIsNotAdminGatedSoRuntimeResolutionWorks() {
        assertThat(findMethod("getAiDefaultChatModelApiKey").getAnnotation(PreAuthorize.class))
            .as(
                "getAiDefaultChatModelApiKey must NOT have @PreAuthorize — it is runtime key resolution consumed by "
                    + "CatalogChatModel on non-admin and reactive threads with no SecurityContext, and is not exposed "
                    + "by any controller.")
            .isNull();
    }

    @Test
    void testGetAiDefaultEmbeddingModelApiKeyIsNotAdminGatedSoRuntimeResolutionWorks() {
        assertThat(findMethod("getAiDefaultEmbeddingModelApiKey").getAnnotation(PreAuthorize.class))
            .as(
                "getAiDefaultEmbeddingModelApiKey must NOT have @PreAuthorize — it is runtime key resolution consumed "
                    + "by CatalogEmbeddingModel and the RAG QuestionAnswer advisor on non-admin and reactive threads "
                    + "with no SecurityContext, and is not exposed by any controller.")
            .isNull();
    }

    @Test
    void testUpdateAiProviderRequiresAdmin() {
        List<Method> matches = findMethods("updateAiProvider");

        assertThat(matches)
            .as("Expected both updateAiProvider overloads on the EE AiProviderFacadeImpl")
            .hasSize(2);

        for (Method method : matches) {
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

            assertThat(preAuthorize)
                .as("updateAiProvider overload must have @PreAuthorize(hasAuthority(\"ROLE_ADMIN\"))")
                .isNotNull();

            assertThat(preAuthorize.value())
                .isEqualTo(ADMIN_EXPRESSION);
        }
    }

    @Test
    void testGetAiChatProviderCatalogIsNotAdminGatedSoTheGatewayCanRead() {
        assertThat(findMethod("getAiChatProviderCatalog").getAnnotation(PreAuthorize.class))
            .as(
                "getAiChatProviderCatalog must NOT have @PreAuthorize — it is shared with the AI gateway and must "
                    + "remain readable by non-admins.")
            .isNull();
    }

    @Test
    void testGetAiDefaultChatModelIsNotAdminGatedSoTheGatewayCanRead() {
        List<Method> matches = findMethods("getAiDefaultChatModel");

        assertThat(matches)
            .as("Expected both getAiDefaultChatModel overloads on the EE AiProviderFacadeImpl")
            .hasSize(2);

        for (Method method : matches) {
            assertThat(method.getAnnotation(PreAuthorize.class))
                .as(
                    "getAiDefaultChatModel must NOT have @PreAuthorize — it is shared with the AI gateway and must "
                        + "remain readable by non-admins.")
                .isNull();
        }
    }

    @Test
    void testGetAiDefaultEmbeddingModelIsNotAdminGatedSoTheGatewayCanRead() {
        assertThat(findMethod("getAiDefaultEmbeddingModel").getAnnotation(PreAuthorize.class))
            .as(
                "getAiDefaultEmbeddingModel must NOT have @PreAuthorize — it is shared with the AI gateway and must "
                    + "remain readable by non-admins.")
            .isNull();
    }

    private static void assertAdminOnly(String methodName) {
        PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "Method '%s' must have @PreAuthorize(hasAuthority(\"ROLE_ADMIN\")); dropping it would silently let "
                    + "every authenticated user perform an admin-only operation.",
                methodName)
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Method '%s' @PreAuthorize expression must require ROLE_ADMIN", methodName)
            .isEqualTo(ADMIN_EXPRESSION);
    }

    private static Method findMethod(String methodName) {
        List<Method> matches = findMethods(methodName);

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on the EE AiProviderFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }

    private static List<Method> findMethods(String methodName) {
        return Arrays.stream(AiProviderFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();
    }
}
