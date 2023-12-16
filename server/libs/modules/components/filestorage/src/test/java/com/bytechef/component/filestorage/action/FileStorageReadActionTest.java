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

package com.bytechef.component.filestorage.action;

import static com.bytechef.component.filestorage.constant.FileStorageConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ParameterMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class FileStorageReadActionTest {

    private static final ActionContext context = Mockito.mock(ActionContext.class);

    @Disabled
    @Test
    public void testPerformRead() {
        ActionContext.FileEntry fileEntry = Mockito.mock(ActionContext.FileEntry.class);
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.getRequiredFileEntry(Mockito.eq(FILE_ENTRY)))
            .thenReturn(fileEntry);

        FileStorageReadAction.perform(parameterMap, parameterMap, context);

        ArgumentCaptor<ActionContext.FileEntry> fileEntryArgumentCaptor =
            ArgumentCaptor.forClass(ActionContext.FileEntry.class);

        Mockito.verify(context)
            .file(file -> file.readToString(fileEntryArgumentCaptor.capture()));

        assertThat(fileEntryArgumentCaptor.getValue()).isEqualTo(fileEntry);
    }
}
