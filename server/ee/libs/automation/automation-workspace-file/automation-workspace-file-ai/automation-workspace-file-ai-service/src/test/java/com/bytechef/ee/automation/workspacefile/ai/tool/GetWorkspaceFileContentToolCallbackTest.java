/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workspacefile.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.service.WorkspaceFileFacade;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class GetWorkspaceFileContentToolCallbackTest {

    @Mock
    private WorkspaceFileFacade facade;

    @Test
    void testCallHappyPath() {
        WorkspaceFile file = new WorkspaceFile();

        file.setId(123L);
        file.setName("spec.md");
        file.setMimeType("text/markdown");
        file.setSizeBytes(11L);

        when(facade.findById(123L)).thenReturn(file);
        when(facade.downloadContent(123L))
            .thenReturn(new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)));

        GetWorkspaceFileContentToolCallback callback = new GetWorkspaceFileContentToolCallback(facade);

        String result = callback.call("{\"id\":123}");

        assertThat(result).contains("\"id\":123");
        assertThat(result).contains("\"name\":\"spec.md\"");
        assertThat(result).contains("\"content\":\"hello world\"");
    }
}
