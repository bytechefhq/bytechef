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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.bytechef.component.definition.ActionContext;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.ImportFormat;
import com.dropbox.core.v2.files.PaperCreateUploader;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
class DropboxCreateNewTextFileActionTest extends AbstractDropboxActionTest {

    @Test
    void testPerform() throws DbxException, IOException {
        PaperCreateUploader paperCreateUploader = Mockito.mock(PaperCreateUploader.class);

        Mockito
            .when(filesRequests.paperCreate(any(), eq(ImportFormat.PLAIN_TEXT)))
            .thenReturn(paperCreateUploader);

        DropboxCreateNewTextFileAction.perform(
            parameters, parameters, Mockito.mock(ActionContext.class));

        ArgumentCaptor<ImportFormat> importFormatArgumentCaptor = ArgumentCaptor.forClass(ImportFormat.class);

        then(filesRequests)
            .should(times(1))
            .paperCreate(stringArgumentCaptorSource.capture(), importFormatArgumentCaptor.capture());

        Assertions.assertEquals(DESTINATION_STUB + "/" + FILENAME_STUB + ".paper", stringArgumentCaptorSource.getValue());
        Assertions.assertEquals(ImportFormat.PLAIN_TEXT, importFormatArgumentCaptor.getValue());

        ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);

        then(paperCreateUploader)
            .should(times(1))
            .uploadAndFinish(inputStreamArgumentCaptor.capture());

        InputStream inputStream = inputStreamArgumentCaptor.getValue();

        Assertions.assertEquals(-1, inputStream.read(), "Input stream must be empty!");
    }
}
