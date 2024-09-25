/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.codeworkflow.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import java.net.URL;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface CodeWorkflowFileStorage {

    void deleteCodeWorkflowDefinition(@NonNull FileEntry definitionFile);

    void deleteCodeWorkflowFile(@NonNull FileEntry codeFile);

    String readCodeWorkflowDefinition(@NonNull FileEntry definitionFile);

    URL getCodeWorkflowFileURL(@NonNull FileEntry codeFile);

    FileEntry storeCodeWorkflowDefinition(@NonNull String filename, @NonNull String definition);

    FileEntry storeCodeWorkflowFile(String filename, @NonNull byte[] bytes);
}
