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

package com.bytechef.platform.workflow.test.web.rest;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.file.storage.TempFileStorage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
final class TestAttachmentUtils {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("data:image/[^;]+;base64,([^\\s]+)");
    private static final Pattern TEXT_PATTERN = Pattern.compile(
        "<attachment[^>]*>(.*?)</attachment>", Pattern.DOTALL);

    private TestAttachmentUtils() {
    }

    static List<FileEntry> getFileEntries(TempFileStorage tempFileStorage, Map<String, Object> parameters) {
        List<Map<String, Object>> attachments = MapUtils.getRequiredList(
            parameters, "attachments", new TypeReference<>() {});

        List<FileEntry> fileEntries = new ArrayList<>();

        for (Map<String, Object> attachment : attachments) {
            List<Map<String, String>> content = MapUtils.getRequiredList(
                attachment, "content", new TypeReference<>() {});

            if (content.isEmpty()) {
                continue;
            }

            String contentType = MapUtils.getRequiredString(attachment, "contentType");
            String name = MapUtils.getRequiredString(attachment, "name");

            if (contentType.startsWith("text/")) {
                String textValue = MapUtils.getString(content.getFirst(), "text");

                if (textValue != null) {
                    Matcher matcher = TEXT_PATTERN.matcher(textValue);

                    if (matcher.find()) {
                        String text = matcher.group(1);

                        fileEntries.add(tempFileStorage.storeFileContent(name, text));
                    }
                }
            } else {
                String imageValue = MapUtils.getString(content.getFirst(), "image");

                if (imageValue != null) {
                    Matcher matcher = IMAGE_PATTERN.matcher(imageValue);

                    if (matcher.find()) {
                        fileEntries.add(tempFileStorage.storeFileContent(
                            name, new ByteArrayInputStream(EncodingUtils.base64Decode(matcher.group(1)))));
                    }
                }
            }
        }

        return fileEntries;
    }

}
