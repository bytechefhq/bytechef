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

package com.bytechef.component.resend.util;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class ResendUtils {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    private ResendUtils() {
    }

    public static List<Map<String, String>> getAttachments(List<FileEntry> fileEntries, ActionContext actionContext) {
        return fileEntries.stream()
            .map(fileEntry -> {
                byte[] fileBytes = actionContext.file(file -> file.readAllBytes(fileEntry));
                return Map.of("filename", fileEntry.getName(), "content", ENCODER.encodeToString(fileBytes));
            })
            .toList();
    }
}
