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

package com.bytechef.component.dropbox.action;

import static com.bytechef.component.dropbox.constant.DropboxConstants.FILE_ENTRY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.bytechef.component.definition.ActionContext;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.UploadBuilder;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
public class DropboxUploadFileActionTest extends AbstractDropboxActionTest {

    @Test
    public void testPerform() throws IOException, DbxException {
        Mockito
            .when(parameters.getRequiredString(FILE_ENTRY))
            .thenReturn(SOURCE_STUB);

        UploadBuilder uploadBuilder = Mockito.mock(UploadBuilder.class);

        Mockito
            .when(filesRequests.uploadBuilder(DESTINATION_STUB))
            .thenReturn(uploadBuilder);

        DropboxUploadFileAction.perform(parameters, parameters, Mockito.mock(ActionContext.class));

        then(filesRequests)
            .should(times(1))
            .uploadBuilder(stringArgumentCaptorA.capture());

        then(uploadBuilder)
            .should(times(1))
            .uploadAndFinish(any());

        Assertions.assertEquals(DESTINATION_STUB, stringArgumentCaptorA.getValue());
    }
}
