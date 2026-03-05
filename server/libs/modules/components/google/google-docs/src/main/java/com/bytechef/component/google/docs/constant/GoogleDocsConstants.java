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

package com.bytechef.component.google.docs.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class GoogleDocsConstants {

    private GoogleDocsConstants() {
    }

    public static final String APPLICATION_VND_GOOGLE_APPS_DOCUMENT = "application/vnd.google-apps.document";
    public static final String BODY = "body";
    public static final String DOCUMENT_ID = "documentId";
    public static final String TITLE = "title";
    public static final String VALUES = "values";

    public static final ModifiableObjectProperty DOCUMENT_OUTPUT_PROPERTY = object()
        .properties(
            string("documentId")
                .description("The ID of the document."),
            string("title")
                .description("The title of the document."),
            array("tabs")
                .description("Tabs that are part of a document.")
                .items(
                    object()
                        .properties(
                            object("tabProperties")
                                .description("The properties of the tab, like ID and title.")
                                .properties(
                                    string("tabId")
                                        .description("The ID of the tab."),
                                    string("title")
                                        .description("The user-visible name of the tab."),
                                    string("parentTabId")
                                        .description(
                                            "The ID of the parent tab. Empty when the current tab is a root-level tab, which means it doesn't have any parents."),
                                    integer("index")
                                        .description("The zero-based index of the tab within the parent."),
                                    integer("nestingLevel")
                                        .description(
                                            "The depth of the tab within the document. Root-level tabs start at 0."),
                                    string("iconEmoji")
                                        .description("The emoji icon displayed with the tab.")),
                            array("childTabs")
                                .description("The child tabs nested within this tab.")
                                .items(object()))),
            string("revisionId")
                .description("The revision ID of the document."),
            string("suggestionsViewMode")
                .description("The suggestions view mode applied to the document."),
            object("body")
                .description("The main body of the document.")
                .properties(
                    array("content")
                        .description("The contents of the body.")
                        .items(
                            object()
                                .properties(
                                    integer("startIndex")
                                        .description(
                                            "The zero-based start index of this structural element, in UTF-16 code units."),
                                    integer("endIndex")
                                        .description(
                                            "The zero-based end index of this structural element, exclusive, in UTF-16 code units.")))));
}
