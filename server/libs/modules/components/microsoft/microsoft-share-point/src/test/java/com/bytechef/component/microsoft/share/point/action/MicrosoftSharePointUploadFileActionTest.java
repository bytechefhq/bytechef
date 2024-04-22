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

package com.bytechef.component.microsoft.share.point.action;

import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class MicrosoftSharePointUploadFileActionTest extends AbstractMicrosoftSharePointActionTest {

    private final FileEntry mockedFileEntry = mock(FileEntry.class);

    @Test
    void testPerform() {
        when(mockedParameters.getRequiredFileEntry(FILE))
            .thenReturn(mockedFileEntry);

        Object result =
            MicrosoftSharePointUploadFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(mockedFileEntry, body.getContent());
    }

}
