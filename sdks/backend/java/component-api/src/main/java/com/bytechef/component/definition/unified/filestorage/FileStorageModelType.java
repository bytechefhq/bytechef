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

package com.bytechef.component.definition.unified.filestorage;

import com.bytechef.component.definition.UnifiedApiDefinition;

/**
 * Enumerates the unified model types exposed by the file storage category of the unified API, covering the storage and
 * access-control entities that cloud file-storage providers have in common.
 *
 * @author Ivica Cardic
 */
public enum FileStorageModelType implements UnifiedApiDefinition.ModelType {

    /** A top-level storage volume that contains folders and files. */
    DRIVE,
    /** An individual stored file. */
    FILE,
    /** A directory that groups files and other folders. */
    FOLDER,
    /** A group of users used for sharing and permissions. */
    GROUP,
    /** A user of the file storage system. */
    USER
}
