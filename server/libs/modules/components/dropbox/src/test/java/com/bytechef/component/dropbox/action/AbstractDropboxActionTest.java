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

import static com.bytechef.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;
import static com.bytechef.component.dropbox.constant.DropboxConstants.DESTINATION_FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SOURCE_FILENAME;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.dropbox.util.DropboxUtils;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
public abstract class AbstractDropboxActionTest {
    static final String DESTINATION_STUB = "destinationPathStub";
    static final String SOURCE_STUB = "sourcePathStub";

    protected MockedStatic<DropboxUtils> dropboxUtils;
    protected DbxUserFilesRequests filesRequests;
    protected Parameters parameters;
    protected ArgumentCaptor<String> stringArgumentCaptorA;
    protected ArgumentCaptor<String> stringArgumentCaptorB;

    @BeforeEach
    public void beforeEach() {
        dropboxUtils = Mockito.mockStatic(DropboxUtils.class);

        filesRequests = Mockito.mock(DbxUserFilesRequests.class);

        dropboxUtils
            .when(() -> DropboxUtils.getDbxUserFilesRequests(""))
            .thenReturn(filesRequests);

        parameters = Mockito.mock(Parameters.class);
        stringArgumentCaptorA = ArgumentCaptor.forClass(String.class);
        stringArgumentCaptorB = ArgumentCaptor.forClass(String.class);

        Mockito
            .when(parameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn("");

        Mockito
            .when(parameters.getRequiredString(SOURCE_FILENAME))
            .thenReturn(SOURCE_STUB);

        Mockito
            .when(parameters.getRequiredString(DESTINATION_FILENAME))
            .thenReturn(DESTINATION_STUB);
    }

    @AfterEach
    public void afterEach() {
        dropboxUtils.close();
    }
}
