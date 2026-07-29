/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
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
class CodeWorkflowFileStorageTest {

    private static final String CODE_WORKFLOWS_DIR = "code_workflows";

    @Mock
    private FileStorageService fileStorageService;

    private CodeWorkflowFileStorageImpl codeWorkflowFileStorage;

    @BeforeEach
    void beforeEach() {
        codeWorkflowFileStorage = new CodeWorkflowFileStorageImpl(fileStorageService);
    }

    @Test
    void testReadCodeWorkflowFileContent() {
        FileEntry fileEntry = new FileEntry("workflow.js", "file://test/workflow.js");
        String expectedContent = "console.log('Hello, world!');";

        when(fileStorageService.readFileToString(CODE_WORKFLOWS_DIR, fileEntry)).thenReturn(expectedContent);

        String result = codeWorkflowFileStorage.readCodeWorkflowFileContent(fileEntry);

        assertThat(result).isEqualTo(expectedContent);
    }

    @Test
    void testReadCodeWorkflowFileContentWithUtf8Characters() {
        FileEntry fileEntry = new FileEntry("workflow.py", "file://test/workflow.py");
        String expectedContent = "print('Content with UTF-8: 你好世界 🌍')";

        when(fileStorageService.readFileToString(CODE_WORKFLOWS_DIR, fileEntry))
            .thenReturn(new String(expectedContent.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        String result = codeWorkflowFileStorage.readCodeWorkflowFileContent(fileEntry);

        assertThat(result).isEqualTo(expectedContent);
    }
}
