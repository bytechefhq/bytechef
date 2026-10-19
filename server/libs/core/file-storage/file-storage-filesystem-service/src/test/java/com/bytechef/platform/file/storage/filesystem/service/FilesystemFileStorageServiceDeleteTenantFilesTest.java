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

package com.bytechef.platform.file.storage.filesystem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class FilesystemFileStorageServiceDeleteTenantFilesTest {

    @TempDir
    Path baseDirectory;

    @Test
    void testDeletesOnlyTheGivenTenantDirectory() throws IOException {
        Path nested = Files.createDirectories(baseDirectory.resolve("000001/data/nested"));

        Files.writeString(nested.resolve("file.txt"), "one");

        Path kept = Files.createDirectories(baseDirectory.resolve("000002/data"));

        Files.writeString(kept.resolve("file.txt"), "two");

        FilesystemFileStorageService service = new FilesystemFileStorageService(baseDirectory.toString());

        service.deleteTenantFiles("000001");

        assertThat(baseDirectory.resolve("000001")).doesNotExist();
        assertThat(kept.resolve("file.txt")).exists();
    }

    @Test
    void testMissingTenantDirectoryIsNoOp() {
        FilesystemFileStorageService service = new FilesystemFileStorageService(baseDirectory.toString());

        service.deleteTenantFiles("000009");

        assertThat(baseDirectory).exists();
    }

    @Test
    void testRejectsTenantIdWithPathCharacters() {
        FilesystemFileStorageService service = new FilesystemFileStorageService(baseDirectory.toString());

        assertThatThrownBy(() -> service.deleteTenantFiles("../000002"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
