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

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DbxUserFilesRequests;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxUtils {

    public static DbxUserFilesRequests getDbxUserFilesRequests(String accessToken) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder(CLIENT_IDENTIFIER)
            .build();

        return new DbxClientV2(config, accessToken).files();
    }

    private DropboxUtils() {
    }
}
