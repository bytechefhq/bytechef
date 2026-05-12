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

package com.bytechef.automation.assetfile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.repository.AssetFileRepository;
import com.bytechef.automation.assetfile.repository.WorkspaceAssetFileRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AssetFileServiceTest {

    @Mock
    private AssetFileRepository repository;

    @Mock
    private WorkspaceAssetFileRepository workspaceRepository;

    private AssetFileService service;

    @BeforeEach
    void setUp() {
        service = new AssetFileServiceImpl(repository, workspaceRepository);
    }

    @Test
    void testCreatePersistsAndLinksToWorkspace() {
        AssetFile input = new AssetFile();

        input.setName("spec.md");

        AssetFile saved = new AssetFile();

        saved.setId(42L);
        saved.setName("spec.md");

        when(repository.save(input)).thenReturn(saved);

        AssetFile result = service.create(input, 7L);

        assertThat(result.getId()).isEqualTo(42L);

        verify(workspaceRepository).save(argThat(
            link -> link.getWorkspaceId()
                .equals(7L)
                && link.getAssetFileId()
                    .equals(42L)));
    }

    @Test
    void testFindByIdThrowsWhenMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
