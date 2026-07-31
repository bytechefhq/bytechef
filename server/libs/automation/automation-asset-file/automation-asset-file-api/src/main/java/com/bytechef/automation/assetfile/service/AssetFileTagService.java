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

import com.bytechef.platform.tag.domain.Tag;
import java.util.List;

/**
 * Service for accessing tags associated with Workspace Files.
 *
 * @author Ivica Cardic
 */
public interface AssetFileTagService {

    /**
     * Retrieves the distinct tags assigned to the asset files of the given workspace.
     *
     * @param workspaceId the id of the workspace whose asset file tags are to be retrieved
     * @return a list of Tag objects assigned to the workspace's asset files
     */
    List<Tag> getAllTags(long workspaceId);

    /**
     * Updates the tags associated with a specific workspace file.
     *
     * @param assetFileId the unique identifier of the workspace file whose tags are to be updated
     * @param tags        a list of Tag objects representing the new set of tags to associate with the workspace file
     */
    void updateTags(long assetFileId, List<Tag> tags);
}
