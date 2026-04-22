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

package com.bytechef.automation.workspacefile.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceFileFileStorageImplTest {

    @Mock
    private FileStorageService fileStorageService;

    private WorkspaceFileFileStorage fileStorage;

    @BeforeEach
    void setUp() {
        fileStorage = new WorkspaceFileFileStorageImpl(fileStorageService);
    }

    @Test
    void testStoreFileString() {
        FileEntry expected = new FileEntry("spec.md", "url");

        when(fileStorageService.storeFileContent("workspace-files", "spec.md", "hello", true))
            .thenReturn(expected);

        FileEntry actual = fileStorage.storeFile("spec.md", "hello");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testStoreFileStream() {
        FileEntry expected = new FileEntry("spec.md", "url");
        InputStream data = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));

        when(
            fileStorageService.storeFileContent(eq("workspace-files"), eq("spec.md"), any(InputStream.class), eq(true)))
                .thenReturn(expected);

        FileEntry actual = fileStorage.storeFile("spec.md", data);

        assertThat(actual).isEqualTo(expected);
    }
}
