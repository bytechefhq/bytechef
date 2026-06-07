/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} annotation on {@link WorkspaceKnowledgeBaseSourceFacadeImpl#refreshNow}. The admin-only
 * guard was moved off {@code KnowledgeBaseSourceGraphQlController} onto the facade so it protects every caller of the
 * facade, not just the GraphQL entry point; this reflection test catches a refactor that silently drops it. Runtime
 * enforcement of the expression is a Spring-wide guarantee of {@code @EnableMethodSecurity}.
 *
 * @author Ivica Cardic
 */
class WorkspaceKnowledgeBaseSourceFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";

    @Test
    void testRefreshNowRequiresAdmin() {
        PreAuthorize preAuthorize = findMethod("refreshNow").getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "refreshNow must have @PreAuthorize(hasAuthority(\"ROLE_ADMIN\")); dropping it would silently let "
                    + "every authenticated user trigger an admin-only knowledge base source refresh.")
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("refreshNow @PreAuthorize expression must require ROLE_ADMIN")
            .isEqualTo(ADMIN_EXPRESSION);
    }

    private static Method findMethod(String methodName) {
        List<Method> matches = Arrays.stream(WorkspaceKnowledgeBaseSourceFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on WorkspaceKnowledgeBaseSourceFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}
