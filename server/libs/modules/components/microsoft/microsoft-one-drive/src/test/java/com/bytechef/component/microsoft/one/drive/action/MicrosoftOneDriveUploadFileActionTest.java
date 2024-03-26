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

package com.bytechef.component.microsoft.one.drive.action;

import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.FileEntry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class MicrosoftOneDriveUploadFileActionTest extends AbstractMicrosoftOneDriveActionTest {

    protected ArgumentCaptor<Http.Body> bodyArgumentCaptor =
        ArgumentCaptor.forClass(Http.Body.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);

    @Test
    void testPerform() {
        Map<String, String> map = Map.of("key", "value");

        when(mockedParameters.getRequiredFileEntry(FILE))
            .thenReturn(mockedFileEntry);

        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        Object result = MicrosoftOneDriveUploadFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(map, result);

        Http.Body body = Http.Body.of(mockedFileEntry);

        Http.Body value = bodyArgumentCaptor.getValue();

        assertEquals(body.getContent(), value.getContent());
        assertEquals(body.getContentType(), value.getContentType());
        assertEquals(body.getMimeType(), value.getMimeType());
    }
}
