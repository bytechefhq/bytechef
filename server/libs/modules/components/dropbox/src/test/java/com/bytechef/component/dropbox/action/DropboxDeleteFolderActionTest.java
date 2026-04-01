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

package com.bytechef.component.dropbox.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.dropbox.util.DropboxUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class DropboxDeleteFolderActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);

    @Test
    void testPerform() {
        try (MockedStatic<DropboxUtils> dropboxUtilsMockedStatic = mockStatic(DropboxUtils.class)) {
            dropboxUtilsMockedStatic.when(() -> DropboxUtils.delete(
                parametersArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            Object result = DropboxDeleteFolderAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}
