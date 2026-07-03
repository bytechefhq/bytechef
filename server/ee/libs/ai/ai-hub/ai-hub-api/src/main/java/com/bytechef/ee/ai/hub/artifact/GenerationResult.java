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

package com.bytechef.ee.ai.hub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;

/**
 * What an {@link ArtifactGenerator} returns after a successful run. The tool callback layer rewraps this into a
 * tool-result JSON envelope (currently shaped to drive the existing {@code OpenFileTabToolCallback} chip-rendering
 * path: {@code id}, {@code name}, {@code format}, plus {@code taskLinked} so the chat surface can flag the rare
 * unlinked-file case to the user).
 *
 * @param assetFileId the primary key of the persisted {@code asset_file} row.
 * @param filename    the final file name written (may include a generator-supplied extension).
 * @param format      the format the generator persisted; used by the chat surface to render the chip icon.
 * @param taskLinked  {@code true} when the {@code (taskId, fileId, AUTHORED)} join was recorded. {@code false} when the
 *                    request carried no task id (first-turn race) — the file still exists but won't appear in the
 *                    task's "Files" panel until something attaches it.
 */
public record GenerationResult(
    long assetFileId,
    String filename,
    AssetFileFormat format,
    boolean taskLinked) {
}
