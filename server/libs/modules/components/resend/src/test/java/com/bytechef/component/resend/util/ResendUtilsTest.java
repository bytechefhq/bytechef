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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class ResendUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);

    @Test
    void testGetAttachments() {
        Base64.Encoder encoder = Base64.getEncoder();
        List<FileEntry> fileEntries = List.of(mockedFileEntry);
        byte[] bytes = {};

        when(mockedFileEntry.getName())
            .thenReturn("fileName");
        when(mockedContext.file(any()))
            .thenReturn(bytes);

        List<Map<String, String>> result = ResendUtils.getAttachments(fileEntries, mockedContext);

        assertEquals(1, result.size());

        Map<String, String> fileEntry = result.getFirst();

        assertEquals("fileName", fileEntry.get("filename"));
        assertEquals(encoder.encodeToString(bytes), fileEntry.get("content"));
    }
}
