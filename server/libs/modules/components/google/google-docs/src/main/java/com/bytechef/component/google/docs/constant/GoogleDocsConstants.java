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

package com.bytechef.component.google.docs.constant;

/**
 * @author Monika Kušter
 */
public class GoogleDocsConstants {

    private GoogleDocsConstants() {
    }

    public static final String APPLICATION_VND_GOOGLE_APPS_DOCUMENT = "application/vnd.google-apps.document";
    public static final String BODY = "body";
    public static final String CREATE_DOCUMENT = "createDocument";
    public static final String CREATE_DOCUMENT_DESCRIPTION = "Create a document on Google Docs.";
    public static final String CREATE_DOCUMENT_TITLE = "Create Document";
    public static final String CREATE_DOCUMENT_FROM_TEMPLATE = "createDocumentFromTemplate";
    public static final String CREATE_DOCUMENT_FROM_TEMPLATE_DESCRIPTION =
        "Creates a new document based on an existing one and can replace any placeholder variables found in your " +
            "template document, like [[name]], [[email]], etc.";
    public static final String CREATE_DOCUMENT_FROM_TEMPLATE_TITLE = "Create Document From Template";
    public static final String DOCUMENT_ID = "documentId";
    public static final String GET_DOCUMENT = "getDocument";
    public static final String GET_DOCUMENT_DESCRIPTION = "Retrieve a specified document from your Google Drive.";
    public static final String GET_DOCUMENT_TITLE = "Get Document";
    public static final String IMAGES = "images";
    public static final String TITLE = "title";
    public static final String VALUES = "values";
}
