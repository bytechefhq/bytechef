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

package com.bytechef.component.ai.vectorstore.reader.util;

import static com.bytechef.component.definition.ComponentDsl.fileEntry;

import com.bytechef.component.definition.Property;

/**
 * * @author Ivica Cardic
 */
public class DocumentReaderConstants {

    public static final String DOCUMENT = "document";
    public static final String JSON_KEYS_TO_USE = "jsonKeysToUse";

    public static final Property DOCUMENT_PROPERTY = fileEntry(DOCUMENT)
        .required(true);
}
