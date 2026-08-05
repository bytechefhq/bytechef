/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CodeWorkflowTaskExecutorTest {

    private static final String AUTOMATION_JAVASCRIPT_SOURCE = """
        ({
            name: 'test-project',
            workflows: [
                {
                    name: 'my-workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: () => 'automation result'
                        }
                    ]
                }
            ]
        })
        """;

    private static final String EMBEDDED_JAVASCRIPT_SOURCE = """
        ({
            componentName: 'test-component',
            componentVersion: 1,
            workflows: [
                {
                    name: 'my-workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: () => 'embedded result'
                        }
                    ]
                }
            ]
        })
        """;

    @TempDir
    private Path tempDir;

    private final ActionDefinitionService actionDefinitionService = mock(ActionDefinitionService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);

    @Test
    void testExecutePerformForAutomation() throws Exception {
        CodeWorkflowTaskExecutor codeWorkflowTaskExecutor = createExecutor(
            "automation.js", AUTOMATION_JAVASCRIPT_SOURCE);

        Object result = codeWorkflowTaskExecutor.executePerform(
            "code-workflow-container-uuid", "my-workflow", "my-task", PlatformType.AUTOMATION,
            ParametersFactory.create(Map.of()), Map.of(), mock(ActionContext.class));

        assertEquals("automation result", result);
    }

    @Test
    void testExecutePerformForEmbedded() throws Exception {
        CodeWorkflowTaskExecutor codeWorkflowTaskExecutor = createExecutor(
            "integration.js", EMBEDDED_JAVASCRIPT_SOURCE);

        Object result = codeWorkflowTaskExecutor.executePerform(
            "code-workflow-container-uuid", "my-workflow", "my-task", PlatformType.EMBEDDED,
            ParametersFactory.create(Map.of()), Map.of(), mock(ActionContext.class));

        assertEquals("embedded result", result);
    }

    @Test
    void testExecutePerformThreadsTaskContextToScriptTask() throws Exception {
        String contextJavaScriptSource = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {
                                name: 'my-task',
                                perform: (context) => context.component.component1.action1({x: 1}, 'connection1')
                            }
                        ]
                    }
                ]
            })
            """;

        CodeWorkflowTaskExecutor codeWorkflowTaskExecutor = createExecutor(
            "automation-context.js", contextJavaScriptSource);

        ComponentConnection componentConnection = new ComponentConnection("component1", 1, 1L, Map.of(), null);

        ActionContext actionContext = mock(ActionContext.class);

        when(componentDefinitionService.getComponentDefinition("component1", null))
            .thenReturn(new ComponentDefinition("component1"));
        when(
            actionDefinitionService.executePerformForPolyglot(
                eq("component1"), eq(0), eq("action1"), eq(Map.of("x", 1)), eq(componentConnection), isNull(),
                eq(actionContext)))
                    .thenReturn("component result");

        Object result = codeWorkflowTaskExecutor.executePerform(
            "code-workflow-container-uuid", "my-workflow", "my-task", PlatformType.AUTOMATION,
            ParametersFactory.create(Map.of()), Map.of("connection1", componentConnection), actionContext);

        assertEquals("component result", result);
    }

    @Test
    void testCreateTaskContextThreadsComponentConnectionsAndActionContext() throws Exception {
        CodeWorkflowTaskExecutor codeWorkflowTaskExecutor = createExecutor(
            "automation.js", AUTOMATION_JAVASCRIPT_SOURCE);

        ComponentConnection componentConnection = new ComponentConnection("component1", 1, 1L, Map.of(), null);

        Map<String, ComponentConnection> componentConnections = Map.of("connection1", componentConnection);

        ActionContext actionContext = mock(ActionContext.class);

        when(componentDefinitionService.getComponentDefinition("component1", null))
            .thenReturn(new ComponentDefinition("component1"));

        CodeWorkflowTaskContext taskContext =
            codeWorkflowTaskExecutor.createTaskContext(componentConnections, actionContext);

        taskContext.component("component1", "action1", Map.of(), "connection1");

        verify(actionDefinitionService).executePerformForPolyglot(
            eq("component1"), eq(0), eq("action1"), eq(Map.of()), eq(componentConnection), isNull(),
            eq(actionContext));
    }

    private CodeWorkflowTaskExecutor createExecutor(String scriptFileName, String scriptSource) throws IOException {
        Path scriptPath = tempDir.resolve(scriptFileName);

        Files.writeString(scriptPath, scriptSource);

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(anyString())).thenReturn(codeWorkflowContainer);

        CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);

        URI scriptUri = scriptPath.toUri();

        when(codeWorkflowFileStorage.getCodeWorkflowFileURL(any())).thenReturn(scriptUri.toURL());

        return new CodeWorkflowTaskExecutor(
            actionDefinitionService, new ApplicationProperties(), mock(CacheManager.class),
            codeWorkflowContainerService, codeWorkflowFileStorage, componentDefinitionService);
    }
}
