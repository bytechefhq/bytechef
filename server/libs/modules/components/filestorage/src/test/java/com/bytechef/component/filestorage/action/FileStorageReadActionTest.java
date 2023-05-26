
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.util.MapValueUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static com.bytechef.component.filestorage.constant.FileStorageConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
public class FileStorageReadActionTest {

    private static final Context context = Mockito.mock(Context.class);

    @Test
    public void testPerformRead() {
        try (MockedStatic<MapValueUtils> mockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            Context.FileEntry fileEntry = Mockito.mock(Context.FileEntry.class);

            mockedStatic.when(() -> MapValueUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(fileEntry);

            FileStorageReadAction.perform(Map.of(), context);

            ArgumentCaptor<Context.FileEntry> fileEntryArgumentCaptor =
                ArgumentCaptor.forClass(Context.FileEntry.class);

            Mockito.verify(context)
                .readFileToString(fileEntryArgumentCaptor.capture());

            assertThat(fileEntryArgumentCaptor.getValue()).isEqualTo(fileEntry);
        }
    }
}
