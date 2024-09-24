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

package com.bytechef.component.text.helper.constant;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Ivica Cardic
 */
public class TextHelperConstants {

    public static final String ATTRIBUTE = "attribute";
    public static final String BASE_64_DECODE = "base64Decode";
    public static final String CONTENT = "content";
    public static final String DELIMITER = "delimiter";
    public static final String ENCODING_SCHEMA = "encodingSchema";
    public static final String ENCODING_SCHEMA_BASE64 = "base64";
    public static final String ENCODING_SCHEMA_BASE64URL = "base64Url";
    public static final String EXTRACT_CONTENT_FROM_HTML = "extractContentFromHtml";
    public static final String HTML = "html";
    public static final String MARKDOWN = "markdown";
    public static final String TEXT_HELPER = "textHelper";
    public static final String QUERY_SELECTOR = "querySelector";
    public static final String REPLACE_ONLY_FIRST = "replaceOnlyFirst";
    public static final String REPLACE_VALUE = "replaceValue";
    public static final String RETURN_VALUE = "returnValue";
    public static final String RETURN_ARRAY = "returnArray";
    public static final String SEARCH_VALUE = "searchValue";
    public static final String SEPARATOR = "separator";
    public static final String TEXT = "text";
    public static final String TEXTS = "texts";

    public static final ModifiableStringProperty TEXT_PROPERTY = string(TEXT)
        .label("Text")
        .controlType(ControlType.TEXT_AREA)
        .required(true);
    public static final String EXPRESSION = "expression";
}
