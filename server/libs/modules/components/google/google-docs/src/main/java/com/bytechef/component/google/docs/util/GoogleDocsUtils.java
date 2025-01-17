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

package com.bytechef.component.google.docs.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.drive.Drive;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleDocsUtils {

    private GoogleDocsUtils() {
    }

    public static Document createDocument(String title, Docs docs) throws IOException {
        return docs
            .documents()
            .create(new Document().setTitle(title))
            .execute();
    }

    public static List<Option<String>> getDocsIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext actionContext) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        return drive.files()
            .list()
            .setQ("mimeType = 'application/vnd.google-apps.document' and trashed = false")
            .execute()
            .getFiles()
            .stream()
            .map(file -> (Option<String>) option(file.getName(), file.getId()))
            .toList();
    }

    public static void writeToDocument(Docs docs, String documentId, List<Request> requests) throws IOException {
        docs
            .documents()
            .batchUpdate(documentId, new BatchUpdateDocumentRequest().setRequests(requests))
            .execute();
    }
}
