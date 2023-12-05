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

import static com.bytechef.component.dropbox.constant.DropboxConstants.DESTINATION_FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SOURCE_FILENAME;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;

import com.bytechef.component.dropbox.util.DropboxUtils;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
abstract class DropboxActionTestAbstract {
    static final String DESTINATION_STUB = "destinationPathStub";
    static final String SOURCE_STUB = "sourcePathStub";
    static ParameterMap parameterMap;
    static DbxUserFilesRequests filesRequests;
    static ArgumentCaptor<String> stringArgumentCaptorA;
    static ArgumentCaptor<String> stringArgumentCaptorB;
    protected MockedStatic<DropboxUtils> dropboxUtils;

    @BeforeAll
    static void beforeAll() {
        parameterMap = Mockito.mock(ParameterMap.class);
        filesRequests = Mockito.mock(DbxUserFilesRequests.class);

        stringArgumentCaptorA = ArgumentCaptor.forClass(String.class);
        stringArgumentCaptorB = ArgumentCaptor.forClass(String.class);

        Mockito.when(parameterMap.getRequiredString(ACCESS_TOKEN))
            .thenReturn("");

        Mockito.when(parameterMap.getRequiredString(SOURCE_FILENAME))
            .thenReturn(SOURCE_STUB);

        Mockito.when(parameterMap.getRequiredString(DESTINATION_FILENAME))
            .thenReturn(DESTINATION_STUB);
    }

    @BeforeEach
    void beforeEach() {
        dropboxUtils = Mockito.mockStatic(DropboxUtils.class);
        dropboxUtils.when(() -> DropboxUtils.getDbxUserFilesRequests(""))
            .thenReturn(filesRequests);
    }

    @AfterEach
    void afterEach() {
        dropboxUtils.close();
    }
}
