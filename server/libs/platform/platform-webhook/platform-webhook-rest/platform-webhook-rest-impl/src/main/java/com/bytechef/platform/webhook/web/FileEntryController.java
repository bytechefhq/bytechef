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

package com.bytechef.platform.webhook.web;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.file.storage.FilesFileStorage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class FileEntryController {

    private final FilesFileStorage filesFileStorage;

    public FileEntryController(FilesFileStorage filesFileStorage) {
        this.filesFileStorage = filesFileStorage;
    }

    @GetMapping("/file-entries/{id}/content")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getImageDynamicType(@PathVariable("id") String id) {
        FileEntry fileEntry = FileEntry.parse(id);

        return ResponseEntity.ok()
            .contentType(MediaType.asMediaType(MimeType.valueOf(fileEntry.getMimeType())))
            .body(new InputStreamResource(filesFileStorage.getFileStream(fileEntry)));
    }
}
