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

package com.bytechef.component.heygen.action;

import static com.bytechef.component.heygen.constant.HeyGenConstants.FILE_ENTRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class HeyGenUploadAssetActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testPerform() {
        when(mockedParameters.getRequiredFileEntry(FILE_ENTRY))
            .thenReturn(mockedFileEntry);
        when(mockedFileEntry.getMimeType())
            .thenReturn("image/jpeg");

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("data", Map.of("id", "1", "name", "test", "file_type", "image", "folder_id", "2")));

        Object result = HeyGenUploadAssetAction.perform(mockedParameters, null, mockedContext);

        assertEquals(List.of("Content-type", "image/jpeg"), stringArgumentCaptor.getAllValues());

        assertEquals(Map.of("id", "1", "name", "test", "file_type", "image", "folder_id", "2"), result);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(mockedFileEntry, body.getContent());
    }
}
