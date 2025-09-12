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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.SharedTemplate;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivica Cardic
 */
public interface SharedTemplateService {

    /**
     * Retrieves an optional shared template by its universally unique identifier (UUID).
     *
     * @param uuid the universally unique identifier of the shared template to fetch
     * @return an {@code Optional} containing the shared template if found, or an empty {@code Optional} if not found
     */
    Optional<SharedTemplate> fetchSharedTemplate(UUID uuid);

    /**
     * Retrieves a shared template by its universally unique identifier (UUID).
     *
     * @param uuid the universally unique identifier of the shared template
     * @return the shared template associated with the specified UUID
     */
    SharedTemplate getSharedTemplate(UUID uuid);

    /**
     * Saves a shared template with the given UUID and file entry.
     *
     * @param uuid      the universally unique identifier of the shared template
     * @param fileEntry the file entry to associate with the shared template
     * @return the saved shared template
     */
    SharedTemplate save(UUID uuid, FileEntry fileEntry);

    /**
     * Updates an existing shared template with new or modified details.
     *
     * @param sharedTemplate the shared template containing the updated information
     * @return the updated shared template
     */
    SharedTemplate update(SharedTemplate sharedTemplate);
}
