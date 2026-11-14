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

package com.bytechef.component.datatable.trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OutputFunction;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableFunction;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookRequestFunction;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.component.definition.TriggerContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
abstract class AbstractDataTableTriggerTest {

    protected static final long DATA_TABLE_ID = 1051;
    protected static final long DEVELOPMENT_ENVIRONMENT_ID = 0;
    protected static final long ENVIRONMENT_ID = 1;
    protected static final long JOB_PRINCIPAL_ID = 1052;
    protected static final String TABLE_NAME = "conversations";
    protected static final String WEBHOOK_URL = "https://webhook";
    protected static final String WORKFLOW_ID = "workflow-1";
    protected static final String WORKFLOW_UUID = "workflow-uuid";
    protected static final long WORKSPACE_ID = 7;

    protected final DataTableRowService dataTableRowService = mock(DataTableRowService.class);
    protected final DataTableService dataTableService = mock(DataTableService.class);
    protected final DataTableWebhookService dataTableWebhookService = mock(DataTableWebhookService.class);
    protected final DataTableWorkspaceResolver dataTableWorkspaceResolver = mock(DataTableWorkspaceResolver.class);

    @BeforeEach
    void beforeEach() {
        when(dataTableWorkspaceResolver.resolveByWorkflowId(WORKFLOW_ID)).thenReturn(OptionalLong.of(WORKSPACE_ID));
        when(dataTableWorkspaceResolver.resolveByJobPrincipalId(JOB_PRINCIPAL_ID, PlatformType.AUTOMATION))
            .thenReturn(OptionalLong.of(WORKSPACE_ID));
        when(dataTableWorkspaceResolver.resolveByWorkflowUuid(WORKFLOW_UUID)).thenReturn(OptionalLong.of(WORKSPACE_ID));
        when(dataTableService.fetchDataTable(WORKSPACE_ID, TABLE_NAME))
            .thenReturn(Optional.of(new DataTable(DATA_TABLE_ID, TABLE_NAME)));
        when(dataTableService.fetchDataTableInfo(DATA_TABLE_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(createDataTableInfo()));
        when(dataTableService.fetchDataTableInfo(DATA_TABLE_ID, DEVELOPMENT_ENVIRONMENT_ID))
            .thenReturn(Optional.of(createDataTableInfo()));
    }

    @Test
    void testWebhookEnableRegistersTheTableOfTheJobsWorkspace() throws Exception {
        String workflowExecutionId = createWorkflowExecutionId(JOB_PRINCIPAL_ID);

        when(
            dataTableWebhookService.addWebhook(
                DATA_TABLE_ID, workflowExecutionId, WEBHOOK_URL, getWebhookType(), ENVIRONMENT_ID))
                    .thenReturn(42L);

        WebhookEnableOutput webhookEnableOutput = webhookEnable(TABLE_NAME, workflowExecutionId);

        assertThat(webhookEnableOutput.parameters()).isEqualTo(Map.of("webhookId", 42L));
    }

    @Test
    void testWebhookEnableResolvesTheWorkspaceFromTheWorkflowWithoutAJob() throws Exception {
        String workflowExecutionId = createWorkflowExecutionId(0);

        when(
            dataTableWebhookService.addWebhook(
                DATA_TABLE_ID, workflowExecutionId, WEBHOOK_URL, getWebhookType(), ENVIRONMENT_ID))
                    .thenReturn(43L);

        WebhookEnableOutput webhookEnableOutput = webhookEnable(TABLE_NAME, workflowExecutionId);

        assertThat(webhookEnableOutput.parameters()).isEqualTo(Map.of("webhookId", 43L));
    }

    @Test
    void testWebhookEnableMissesATableThatExistsOnlyInAnotherWorkspace() {
        when(dataTableService.fetchDataTable(WORKSPACE_ID, "orders")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookEnable("orders", createWorkflowExecutionId(JOB_PRINCIPAL_ID)))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders' not found in this workspace");
    }

    @Test
    void testWebhookEnableFailsWhenTheWorkspaceCannotBeResolved() {
        when(dataTableWorkspaceResolver.resolveByJobPrincipalId(JOB_PRINCIPAL_ID, PlatformType.AUTOMATION))
            .thenReturn(OptionalLong.empty());

        assertThatThrownBy(() -> webhookEnable(TABLE_NAME, createWorkflowExecutionId(JOB_PRINCIPAL_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to determine the workspace of this workflow");
    }

    @Test
    void testOutputReadsTheTableOfTheWorkflowsWorkspace() throws Exception {
        when(dataTableRowService.listRows(DATA_TABLE_ID, 1, 0, DEVELOPMENT_ENVIRONMENT_ID))
            .thenReturn(List.of(new DataTableRow(7, Map.of())));

        OutputResponse outputResponse = output(TABLE_NAME);

        assertThat(outputResponse.getSampleOutput()).isEqualTo(Map.of("id", 7L));
    }

    protected abstract ModifiableTriggerDefinition createTriggerDefinition();

    protected abstract DataTableWebhookType getWebhookType();

    protected static Map<String, Object> rowContent(String type, Map<String, Object> values) {
        return Map.of("type", type, "table", TABLE_NAME, "payload", Map.of("id", 7, "values", values));
    }

    protected static Object webhookRequest(
        ModifiableTriggerDefinition triggerDefinition, Map<String, Object> content) throws Exception {

        WebhookRequestFunction webhookRequestFunction = triggerDefinition.getWebhookRequest()
            .orElseThrow();

        return webhookRequestFunction.apply(null, null, null, null, new TestWebhookBody(content), null, null, null);
    }

    private static DataTableInfo createDataTableInfo() {
        return new DataTableInfo(DATA_TABLE_ID, TABLE_NAME, WORKSPACE_ID, null, List.of(), Instant.EPOCH);
    }

    private static String createWorkflowExecutionId(long jobPrincipalId) {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, jobPrincipalId, WORKFLOW_UUID, "trigger_1");

        return workflowExecutionId.toString();
    }

    private OutputResponse output(String tableName) throws Exception {
        OutputDefinition outputDefinition = createTriggerDefinition().getOutputDefinition()
            .orElseThrow();

        OutputFunction outputFunction = (OutputFunction) outputDefinition.getOutput()
            .orElseThrow();

        TriggerContext triggerContext = mock(
            TriggerContext.class, withSettings().extraInterfaces(TriggerContextAware.class));

        when(((TriggerContextAware) triggerContext).getWorkflowId()).thenReturn(WORKFLOW_ID);

        return outputFunction.apply(
            MockParametersFactory.create(Map.of("table", tableName)), MockParametersFactory.create(Map.of()),
            triggerContext);
    }

    private WebhookEnableOutput webhookEnable(String tableName, String workflowExecutionId) throws Exception {
        WebhookEnableFunction webhookEnableFunction = createTriggerDefinition().getWebhookEnable()
            .orElseThrow();

        TriggerContext triggerContext = mock(
            TriggerContext.class, withSettings().extraInterfaces(TriggerContextAware.class));

        when(((TriggerContextAware) triggerContext).getEnvironmentId()).thenReturn(ENVIRONMENT_ID);

        return webhookEnableFunction.apply(
            MockParametersFactory.create(Map.of("table", tableName)), MockParametersFactory.create(Map.of()),
            WEBHOOK_URL, workflowExecutionId, triggerContext);
    }

    @SuppressFBWarnings("EI")
    private record TestWebhookBody(Map<String, Object> content) implements WebhookBody {

        @Override
        public Object getContent() {
            return content;
        }

        @Override
        public <T> T getContent(Class<T> valueType) {
            return valueType.cast(content);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getContent(TypeReference<T> valueTypeRef) {
            return (T) content;
        }

        @Override
        public ContentType getContentType() {
            return ContentType.JSON;
        }

        @Override
        public String getMimeType() {
            return "application/json";
        }

        @Override
        public String getRawContent() {
            return String.valueOf(content);
        }
    }
}
