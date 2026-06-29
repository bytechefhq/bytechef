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
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that workspace-scope knowledge-base operations (T21), enforced at the
 * facade tier. Per-id ops resolve the owning workspace via {@code KnowledgeBase:ResourceRole}; list/create take a
 * {@code workspaceId} argument.
 *
 * @author Ivica Cardic
 */
class WorkspaceKnowledgeBaseFacadeAuthorizationTest {

    @Test
    void testGetWorkspaceKnowledgeBasesRequiresViewer() {
        assertExpression("getWorkspaceKnowledgeBases",
            "hasPermission(#workspaceId, 'Workspace', 'KNOWLEDGE_BASE_VIEW')");
    }

    @Test
    void testGetKnowledgeBaseTagsRequiresWorkspaceViewer() {
        assertExpression("getKnowledgeBaseTags", "hasPermission(#workspaceId, 'Workspace', 'KNOWLEDGE_BASE_VIEW')");
    }

    @Test
    void testGetKnowledgeBaseRequiresResourceViewer() {
        assertExpression("getKnowledgeBase", "hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_VIEW')");
    }

    @Test
    void testSearchRequiresResourceViewer() {
        assertExpression("searchKnowledgeBase",
            "hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_VIEW')");
    }

    @Test
    void testCreateRequiresWorkspaceEditor() {
        assertExpression("createWorkspaceKnowledgeBase",
            "hasPermission(#workspaceId, 'Workspace', 'KNOWLEDGE_BASE_CREATE')");
    }

    @Test
    void testUpdateRequiresResourceEditor() {
        assertExpression("updateKnowledgeBase",
            "hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_EDIT')");
    }

    @Test
    void testDeleteRequiresResourceEditor() {
        assertExpression(
            "deleteWorkspaceKnowledgeBase", "hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_EDIT')");
    }

    private static void assertExpression(String methodName, String expression) {
        Method method = null;

        for (Method candidate : WorkspaceKnowledgeBaseFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {
                method = candidate;

                break;
            }
        }

        assertThat(method)
            .as("method %s", methodName)
            .isNotNull();

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }
}
