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

package com.bytechef.component.dropbox.util;

import static com.bytechef.component.dropbox.constant.DropboxConstants.FILE_ENTRY;
import static com.bytechef.component.dropbox.util.DropboxUtils.POST_FILES_UPLOAD_CONTEXT_FUNCTION;
import static com.bytechef.component.dropbox.util.DropboxUtils.getFullPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
class DropboxUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ArgumentCaptor<Map<String, List<String>>> headersArgumentCapture = ArgumentCaptor.forClass(Map.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testGetFullPath() {
        String path1 = "folder1/";
        String filename1 = "file1.txt";

        assertEquals("folder1/file1.txt", getFullPath(path1, filename1));

        String path2 = "folder2";
        String filename2 = "file2.txt";

        assertEquals("folder2/file2.txt", getFullPath(path2, filename2));
    }

    @Test
    void testUploadFile() {
        when(mockedParameters.getRequiredFileEntry(FILE_ENTRY))
            .thenReturn(mockedFileEntry);
        when(mockedContext.http(POST_FILES_UPLOAD_CONTEXT_FUNCTION))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(headersArgumentCapture.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);
        when(mockedContext.json(any()))
            .thenReturn("jsonString");

        Object result = DropboxUtils.uploadFile(mockedParameters, mockedContext, mockedFileEntry);

        assertEquals(mockedObject, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(mockedFileEntry, body.getContent());

        Map<String, List<String>> expectedHeaders = Map.of("Dropbox-API-Arg", List.of("jsonString"));

        assertEquals(expectedHeaders, headersArgumentCapture.getValue());
    }
}
