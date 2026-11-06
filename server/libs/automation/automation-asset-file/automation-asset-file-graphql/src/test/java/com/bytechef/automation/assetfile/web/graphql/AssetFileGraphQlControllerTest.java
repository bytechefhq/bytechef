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

package com.bytechef.automation.assetfile.web.graphql;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.config.AutomationAssetFileQuotaProperties;
import com.bytechef.automation.assetfile.config.AutomationAssetFileSharingProperties;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.assetfile.service.AssetFileTagService;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AssetFileGraphQlControllerTest {

    private static final Long ASSET_FILE_ID = 42L;

    @Mock
    private AssetFileFacade assetFileFacade;

    @Mock
    private AssetFileTagService assetFileTagService;

    @Mock
    private TagService tagService;

    private AssetFileGraphQlController assetFileGraphQlController;

    @BeforeEach
    void setUp() {
        AutomationAssetFileQuotaProperties quotaProperties = new AutomationAssetFileQuotaProperties(
            26_214_400L, 1_073_741_824L, 1_048_576L, 10);
        AutomationAssetFileSharingProperties sharingProperties = new AutomationAssetFileSharingProperties(true);

        assetFileGraphQlController = new AssetFileGraphQlController(
            quotaProperties, sharingProperties, tagService, assetFileFacade, assetFileTagService);
    }

    @Test
    void testUpdateAssetFileTagsDoesNotWriteTagsWhenTheFacadeDeniesTheFile() {
        when(assetFileFacade.findById(ASSET_FILE_ID))
            .thenThrow(new AssetFileNotFoundException("Asset file %d not found".formatted(ASSET_FILE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .as("a file owned by another workspace must be refused before AssetFileTagService is reached")
            .isThrownBy(() -> assetFileGraphQlController.updateAssetFileTags(
                new AssetFileGraphQlController.UpdateAssetFileTagsInput(
                    ASSET_FILE_ID, List.of(new AssetFileGraphQlController.TagInput(null, "confidential")))));

        verifyNoInteractions(assetFileTagService);
    }

    @Test
    void testUpdateAssetFileTagsWritesTagsWhenTheFacadeAllowsTheFile() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);

        when(assetFileFacade.findById(ASSET_FILE_ID)).thenReturn(assetFile);

        assetFileGraphQlController.updateAssetFileTags(
            new AssetFileGraphQlController.UpdateAssetFileTagsInput(
                ASSET_FILE_ID, List.of(new AssetFileGraphQlController.TagInput(null, "reviewed"))));

        verify(assetFileTagService).updateTags(anyLong(), anyList());
    }
}
