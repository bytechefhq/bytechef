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

package com.bytechef.component.dropbox.util;

import static com.bytechef.component.dropbox.constant.DropboxConstants.CLIENT_IDENTIFIER;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeast;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
class DropboxUtilsTest {

    static final String ACCESS_TOKEN_STUB = "accessTokenStub";

    @Test
    void testGetDropboxRequestObject() {
        try (MockedStatic<DbxRequestConfig> dbxRequestConfigMockedStatic = Mockito.mockStatic(DbxRequestConfig.class)) {
            DbxRequestConfig.Builder builder = Mockito.mock(DbxRequestConfig.Builder.class);
            DbxRequestConfig dbxRequestConfig = Mockito.mock(DbxRequestConfig.class);
            DbxUserFilesRequests dbxUserFilesRequests = Mockito.mock(DbxUserFilesRequests.class);

            dbxRequestConfigMockedStatic.when(() -> DbxRequestConfig.newBuilder(CLIENT_IDENTIFIER))
                .thenReturn(builder);
            Mockito.when(builder.build())
                .thenReturn(dbxRequestConfig);

            try (MockedConstruction<DbxClientV2> dbxClientV2MockedConstruction =
                Mockito.mockConstruction(DbxClientV2.class, (dbxClientV2, context) -> {
                    Mockito.when(dbxClientV2.files())
                        .thenReturn(dbxUserFilesRequests);

                    Assertions.assertEquals(ACCESS_TOKEN_STUB, context.arguments()
                        .get(1),
                        "Access token used does not match getDropboxRequestObject() method argument!");
                })) {

                DropboxUtils.getDbxUserFilesRequests(ACCESS_TOKEN_STUB);

                then(dbxClientV2MockedConstruction.constructed()
                    .get(0)).should(atLeast(1))
                        .files();

                Assertions.assertEquals(1, dbxClientV2MockedConstruction.constructed()
                    .size(),
                    "One instance of DbxClientV2 is enough!");
            }
        }
    }
}
