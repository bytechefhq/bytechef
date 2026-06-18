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

package com.bytechef.component.file.storage.action;

import static com.bytechef.component.file.storage.constant.FileStorageConstants.FILE_ENTRY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Marko Kriskovic
 */
public class FileStorageReadBytesActionTest {

    private static final ActionContext context = Mockito.mock(ActionContext.class);

    @Test
    public void testPerformReadBytes() throws Exception {
        Context.File file = Mockito.mock(Context.File.class);
        FileEntry fileEntry = Mockito.mock(FileEntry.class);
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getRequiredFileEntry(Mockito.eq(FILE_ENTRY)))
            .thenReturn(fileEntry);
        Mockito.when(context.file(Mockito.any()))
            .thenAnswer(invocation -> {
                Context.ContextFunction<Context.File, ?> function = invocation.getArgument(0);

                return function.apply(file);
            });

        FileStorageReadBytesAction.perform(parameters, parameters, context);

        Mockito.verify(file)
            .readAllBytes(fileEntry);
    }
}
