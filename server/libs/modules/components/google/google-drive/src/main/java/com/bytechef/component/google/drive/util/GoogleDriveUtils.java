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

package com.bytechef.component.google.drive.util;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Preconditions;
import com.google.api.services.drive.Drive;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Mario Cvjetojevic
 * @author Ivica Cardic
 */
public class GoogleDriveUtils {

    public static Drive getDrive(Parameters connectionParameters) {
        return new Drive.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            new OAuthAuthentication(connectionParameters.getRequiredString(ACCESS_TOKEN)))
                .setApplicationName("Google Drive Component")
                .build();
    }

    public static List<Option<String>> getDriveOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {

        Drive drive = GoogleDriveUtils.getDrive(connectionParameters);

        List<com.google.api.services.drive.model.Drive> drives = drive
            .drives()
            .list()
            .execute()
            .getDrives();

        return drives
            .stream()
            .filter(curDrive -> StringUtils.isNotEmpty(searchText) &&
                StringUtils.startsWith(curDrive.getName(), searchText))
            .map(curDrive -> (Option<String>) option(curDrive.getName(), curDrive.getId()))
            .toList();
    }

    private record OAuthAuthentication(String token)
        implements HttpRequestInitializer, HttpExecuteInterceptor {

        private OAuthAuthentication(String token) {
            this.token = Preconditions.checkNotNull(token);
        }

        public void initialize(HttpRequest request) {
            request.setInterceptor(this);
        }

        public void intercept(HttpRequest request) {
            HttpHeaders httpHeaders = request.getHeaders();

            httpHeaders.set("Authorization", "Bearer " + token);
        }
    }
}
