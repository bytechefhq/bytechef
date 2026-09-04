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

package com.bytechef.platform.component.log;

import com.bytechef.file.storage.service.FileStorageService;

/**
 * {@link LogFileStorageImpl} rooted at the editor log directory. Editor test runs execute in a single JVM, so the
 * per-job write chain and its read barrier hold across the whole run.
 *
 * @author Ivica Cardic
 */
public class EditorLogFileStorageImpl extends LogFileStorageImpl implements EditorLogFileStorage {

    private static final String EDITOR_LOG_FILES_DIR = "editor/logs";

    public EditorLogFileStorageImpl(FileStorageService fileStorageService) {
        super(fileStorageService, EDITOR_LOG_FILES_DIR);
    }
}
