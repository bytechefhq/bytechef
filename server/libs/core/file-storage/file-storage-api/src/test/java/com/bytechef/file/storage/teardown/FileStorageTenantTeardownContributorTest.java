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

package com.bytechef.file.storage.teardown;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.file.storage.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
class FileStorageTenantTeardownContributorTest {

    private final FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);
    private final FileStorageTenantTeardownContributor contributor = new FileStorageTenantTeardownContributor(
        fileStorageService);

    @Test
    void testDeletesTenantFilesAfterSchemaDrop() {
        contributor.afterSchemaDrop("000001");

        verify(fileStorageService).deleteTenantFiles("000001");
    }

    @Test
    void testRejectsInvalidTenantId() {
        assertThatThrownBy(() -> contributor.afterSchemaDrop("../000001"))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(fileStorageService);
    }
}
