/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.facade;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CodeWorkflowContainerFacadeTest {

    private final CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);
    private final CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);
    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade = new CodeWorkflowContainerFacadeImpl(
        codeWorkflowContainerService, codeWorkflowFileStorage, mock(ObjectMapper.class), mock(WorkflowService.class));

    @Test
    void testDeleteDeletesTheContainerAndThenItsWorkflowsFile() {
        FileEntry workflowsFileEntry = new FileEntry("container.js", "file:///code_workflows/container.js");
        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(UUID.randomUUID());

        codeWorkflowContainer.setWorkflows(workflowsFileEntry);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(7L)).thenReturn(codeWorkflowContainer);

        codeWorkflowContainerFacade.delete(7L);

        InOrder inOrder = inOrder(codeWorkflowContainerService, codeWorkflowFileStorage);

        inOrder.verify(codeWorkflowContainerService)
            .delete(7L);
        inOrder.verify(codeWorkflowFileStorage)
            .deleteCodeWorkflowFile(workflowsFileEntry);
    }
}
