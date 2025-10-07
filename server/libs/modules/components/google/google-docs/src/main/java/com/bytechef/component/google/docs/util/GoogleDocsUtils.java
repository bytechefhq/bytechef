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

package com.bytechef.component.google.docs.util;

import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.Request;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleDocsUtils {

    private GoogleDocsUtils() {
    }

    public static Document createDocument(String title, Docs docs) {
        try {
            return docs
                .documents()
                .create(new Document().setTitle(title))
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }

    public static BatchUpdateDocumentResponse writeToDocument(Docs docs, String documentId, List<Request> requests) {
        try {
            return docs
                .documents()
                .batchUpdate(documentId, new BatchUpdateDocumentRequest().setRequests(requests))
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }
}
