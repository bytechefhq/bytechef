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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.constant.PlatformType;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void testExecutePerformForAutomation() throws IOException {
        CodeWorkflowTaskExecutor codeWorkflowTaskExecutor = createExecutor(
            "automation.js", AUTOMATION_JAVASCRIPT_SOURCE);

        Object result = codeWorkflowTaskExecutor.executePerform(
            "code-workflow-container-uuid", "my-workflow", "my-task", PlatformType.AUTOMATION);

        assertEquals("automation result", result);
    }

    @Test
    void testExecutePerformForEmbedded() throws IOException {
        CodeWorkflowTaskExecutor codeWorkflowTaskExecutor = createExecutor(
            "integration.js", EMBEDDED_JAVASCRIPT_SOURCE);

        Object result = codeWorkflowTaskExecutor.executePerform(
            "code-workflow-container-uuid", "my-workflow", "my-task", PlatformType.EMBEDDED);

        assertEquals("embedded result", result);
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
            new ApplicationProperties(), mock(CacheManager.class), codeWorkflowFileStorage,
            codeWorkflowContainerService);
    }
}
