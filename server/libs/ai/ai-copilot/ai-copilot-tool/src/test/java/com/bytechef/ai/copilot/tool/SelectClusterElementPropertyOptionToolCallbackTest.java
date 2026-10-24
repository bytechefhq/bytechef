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

package com.bytechef.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.ai.tool.util.AllowedComponentNamesUtils;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SelectClusterElementPropertyOptionToolCallbackTest {

    private static final String VECTOR_STORE_INPUT =
        "{\"componentName\":\"knowledgeBase\",\"clusterElementName\":\"vectorStore\"," +
            "\"propertyName\":\"knowledgeBaseId\"}";

    private final JsonMapper jsonMapper = new JsonMapper();

    private final PropertyOptionsResolver resolver = new PropertyOptionsResolver(
        new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

    private ToolContext toolContext() {
        return new ToolContext(
            new AgentToolInvocationContext(1L, null, 0L, "thread-1", null).toToolContext());
    }

    @Test
    void testToolNameIsSelectClusterElementPropertyOption() {
        SelectClusterElementPropertyOptionToolCallback callback = new SelectClusterElementPropertyOptionToolCallback(
            mock(ClusterElementDefinitionService.class), mock(ClusterElementDefinitionFacade.class), resolver,
            mock(ToolStateVisibilityMetrics.class), mock(ScopedConnectionGuard.class));

        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("selectClusterElementPropertyOption");
    }

    @Test
    void testRejectsAConnectionTheScopedCallerDoesNotOwnBeforeAnyLookup() {
        ClusterElementDefinitionService service = mock(ClusterElementDefinitionService.class);
        ClusterElementDefinitionFacade facade = mock(ClusterElementDefinitionFacade.class);

        SelectClusterElementPropertyOptionToolCallback callback = new SelectClusterElementPropertyOptionToolCallback(
            service, facade, resolver, mock(ToolStateVisibilityMetrics.class),
            ScopedConnectionTestFixtures.newGuardEntitledTo(List.of(42L)));

        String result = callback.call(
            "{\"componentName\":\"pinecone\",\"clusterElementName\":\"vectorStore\",\"propertyName\":\"index\"," +
                "\"connectionId\":900}",
            ScopedConnectionTestFixtures.scopedToolContext());

        assertThat(result).contains(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);

        verifyNoInteractions(service, facade);
    }

    @Test
    void testReturnsSelectMarkerWithAllKnowledgeBasesOnSuccess() throws Exception {
        ClusterElementDefinitionService service = mock(ClusterElementDefinitionService.class);
        ClusterElementDefinitionFacade facade = mock(ClusterElementDefinitionFacade.class);

        LookupClusterElementPropertyOptionsToolCallbackTest.stubValidClusterElement(
            service, "knowledgeBase", 1, "vectorStore", "knowledgeBaseId");

        when(service.propertyHasOptionsDataSource("knowledgeBase", 1, "vectorStore", "knowledgeBaseId"))
            .thenReturn(true);
        when(service.getPropertyLookupDependsOn("knowledgeBase", 1, "vectorStore", "knowledgeBaseId"))
            .thenReturn(List.of());

        Option productDocs = mock(Option.class);
        Option supportArticles = mock(Option.class);

        when(productDocs.getLabel()).thenReturn("Product docs");
        when(productDocs.getValue()).thenReturn(12L);
        when(supportArticles.getLabel()).thenReturn("Support articles");
        when(supportArticles.getValue()).thenReturn(14L);

        when(facade.executeOptions(
            eq("knowledgeBase"), anyInt(), eq("vectorStore"), eq("knowledgeBaseId"), any(), any(), any(), any(),
            isNull(), any(), any()))
                .thenReturn(List.of(productDocs, supportArticles));

        SelectClusterElementPropertyOptionToolCallback callback = new SelectClusterElementPropertyOptionToolCallback(
            service, facade, resolver, mock(ToolStateVisibilityMetrics.class), mock(ScopedConnectionGuard.class));

        String result = callback.call(VECTOR_STORE_INPUT, toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("kind")
            .asString()).isEqualTo("select-property-option");
        assertThat(node.get("componentName")
            .asString()).isEqualTo("knowledgeBase");
        assertThat(node.get("propertyName")
            .asString()).isEqualTo("knowledgeBaseId");
        assertThat(node.get("options")
            .size()).isEqualTo(2);
        assertThat(node.get("options")
            .get(1)
            .get("label")
            .asString()).isEqualTo("Support articles");
        assertThat(node.get("options")
            .get(1)
            .get("value")
            .asLong()).isEqualTo(14L);
        assertThat(node.get("awaitingAnswer")
            .asBoolean()).isTrue();
        assertThat(node.get("truncated")
            .asBoolean()).isFalse();
    }

    @Test
    void testReturnsAMarkerThatAwaitsNoAnswerWhenNoKnowledgeBaseExists() throws Exception {
        ClusterElementDefinitionService service = mock(ClusterElementDefinitionService.class);
        ClusterElementDefinitionFacade facade = mock(ClusterElementDefinitionFacade.class);

        LookupClusterElementPropertyOptionsToolCallbackTest.stubValidClusterElement(
            service, "knowledgeBase", 1, "vectorStore", "knowledgeBaseId");

        when(service.propertyHasOptionsDataSource("knowledgeBase", 1, "vectorStore", "knowledgeBaseId"))
            .thenReturn(true);
        when(service.getPropertyLookupDependsOn("knowledgeBase", 1, "vectorStore", "knowledgeBaseId"))
            .thenReturn(List.of());
        when(facade.executeOptions(
            eq("knowledgeBase"), anyInt(), eq("vectorStore"), eq("knowledgeBaseId"), any(), any(), any(), any(),
            isNull(), any(), any()))
                .thenReturn(List.of());

        SelectClusterElementPropertyOptionToolCallback callback = new SelectClusterElementPropertyOptionToolCallback(
            service, facade, resolver, mock(ToolStateVisibilityMetrics.class), mock(ScopedConnectionGuard.class));

        JsonNode node = jsonMapper.readTree(callback.call(VECTOR_STORE_INPUT, toolContext()));

        assertThat(node.get("options")
            .size()).isZero();
        assertThat(node.get("awaitingAnswer")
            .asBoolean()).isFalse();
    }

    @Test
    void testReturnsClusterElementNotFoundWithValidNames() throws Exception {
        ClusterElementDefinitionService service = mock(ClusterElementDefinitionService.class);

        ClusterElementDefinition real = mock(ClusterElementDefinition.class);

        when(real.getName()).thenReturn("vectorStore");
        when(service.getClusterElementDefinitions("knowledgeBase", 1)).thenReturn(List.of(real));

        SelectClusterElementPropertyOptionToolCallback callback = new SelectClusterElementPropertyOptionToolCallback(
            service, mock(ClusterElementDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class),
            mock(ScopedConnectionGuard.class));

        String result = callback.call(
            "{\"componentName\":\"knowledgeBase\",\"clusterElementName\":\"questionAnswerRag\"," +
                "\"propertyName\":\"knowledgeBaseId\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asString()).isEqualTo("cluster_element_not_found");
        assertThat(node.get("valid")
            .get(0)
            .asString()).isEqualTo("vectorStore");
    }

    @Test
    void testReturnsPropertyNotFoundWithValidNames() throws Exception {
        ClusterElementDefinitionService service = mock(ClusterElementDefinitionService.class);

        LookupClusterElementPropertyOptionsToolCallbackTest.stubValidClusterElement(
            service, "knowledgeBase", 1, "vectorStore", "knowledgeBaseId", "topK");

        SelectClusterElementPropertyOptionToolCallback callback = new SelectClusterElementPropertyOptionToolCallback(
            service, mock(ClusterElementDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class),
            mock(ScopedConnectionGuard.class));

        String result = callback.call(
            "{\"componentName\":\"knowledgeBase\",\"clusterElementName\":\"vectorStore\",\"propertyName\":\"kbId\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asString()).isEqualTo("property_not_found");

        List<String> validNames = new ArrayList<>();

        node.get("valid")
            .forEach(name -> validNames.add(name.asString()));

        assertThat(validNames).containsExactly("knowledgeBaseId", "topK");
    }

    @Test
    void testReturnsConnectionRequiredWhenConnectionMissing() throws Exception {
        ClusterElementDefinitionService service = mock(ClusterElementDefinitionService.class);
        ClusterElementDefinitionFacade facade = mock(ClusterElementDefinitionFacade.class);

        LookupClusterElementPropertyOptionsToolCallbackTest.stubValidClusterElement(
            service, "pinecone", 1, "vectorStore", "index");

        when(service.propertyHasOptionsDataSource("pinecone", 1, "vectorStore", "index")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("pinecone", 1, "vectorStore", "index")).thenReturn(List.of());
        when(service.clusterElementDefinesConnection("pinecone", 1, "vectorStore")).thenReturn(true);

        SelectClusterElementPropertyOptionToolCallback callback = new SelectClusterElementPropertyOptionToolCallback(
            service, facade, resolver, mock(ToolStateVisibilityMetrics.class), mock(ScopedConnectionGuard.class));

        String result = callback.call(
            "{\"componentName\":\"pinecone\",\"clusterElementName\":\"vectorStore\",\"propertyName\":\"index\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asString()).isEqualTo("connection_required");

        verifyNoInteractions(facade);
    }

    @Test
    void testRejectsBlankPropertyName() throws Exception {
        SelectClusterElementPropertyOptionToolCallback callback = new SelectClusterElementPropertyOptionToolCallback(
            mock(ClusterElementDefinitionService.class), mock(ClusterElementDefinitionFacade.class), resolver,
            mock(ToolStateVisibilityMetrics.class), mock(ScopedConnectionGuard.class));

        String result = callback.call(
            "{\"componentName\":\"knowledgeBase\",\"clusterElementName\":\"vectorStore\",\"propertyName\":\"\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asString()).contains("propertyName is required");
    }

    @Test
    void testRejectsAComponentOutsideTheAllowListBeforeAnyLookup() {
        ClusterElementDefinitionService service = mock(ClusterElementDefinitionService.class);
        ClusterElementDefinitionFacade facade = mock(ClusterElementDefinitionFacade.class);

        SelectClusterElementPropertyOptionToolCallback callback = new SelectClusterElementPropertyOptionToolCallback(
            service, facade, resolver, mock(ToolStateVisibilityMetrics.class), mock(ScopedConnectionGuard.class));

        Map<String, Object> context = new AgentToolInvocationContext(1L, 10L, 0L, "thread-1", null).toToolContext();

        context.put(AllowedComponentNamesUtils.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack"));

        String result = callback.call(VECTOR_STORE_INPUT, new ToolContext(context));

        assertThat(result).contains("This chat can't use knowledgeBase");

        verifyNoInteractions(service, facade);
    }
}
