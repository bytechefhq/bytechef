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

package com.bytechef.component.text.helper.constant;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Ivica Cardic
 */
public class TextHelperConstants {

    public static final String AMOUNT = "amount";
    public static final String ATTRIBUTE = "attribute";
    public static final String CONTENT = "content";
    public static final String CURRENCY = "currency";
    public static final String DECIMAL_DIGITS = "decimalDigits";
    public static final String DECIMAL_SEPARATOR = "decimalSeparator";
    public static final String DELIMITER = "delimiter";
    public static final String ESCAPE_CHARACTER = "escapeCharacter";
    public static final String ESCAPE_CHARACTERS = "escapeCharacters";
    public static final String EXPRESSION = "expression";
    public static final String FIRST_NAME = "firstName";
    public static final String FULL_NAME = "fullName";
    public static final String HAS_MIDDLE_NAME = "hasMiddleName";
    public static final String HTML = "html";
    public static final String IS_FIRST_NAME_FIRST = "isFirstNameFirst";
    public static final String KEY_VALUE_OBJECT = "keyValueObject";
    public static final String LAST_NAME = "lastName";
    public static final String LENGTH = "length";
    public static final String MARKDOWN = "markdown";
    public static final String MATCH_NUMBER = "matchNumber";
    public static final String MIDDLE_NAME = "middleName";
    public static final String NUMBER_OF_CHARACTERS = "numberOfCharacters";
    public static final String OPERATION = "operation";
    public static final String PATTERN = "pattern";
    public static final String PATTERN_END = "patternEnd";
    public static final String PATTERN_START = "patternStart";
    public static final String QUERY_SELECTOR = "querySelector";
    public static final String REGULAR_EXPRESSION = "regularExpression";
    public static final String REPLACE_ONLY_FIRST = "replaceOnlyFirst";
    public static final String REPLACE_VALUE = "replaceValue";
    public static final String RETURN_VALUE = "returnValue";
    public static final String RETURN_ARRAY = "returnArray";
    public static final String SEARCH_VALUE = "searchValue";
    public static final String SEPARATOR = "separator";
    public static final String TEXT = "text";
    public static final String TEXTS = "texts";
    public static final String THOUSANDS_SEPARATOR = "thousandsSeparator";

    public static final ModifiableStringProperty OPERATION_PROPERTY = string(OPERATION)
        .label("Encode or Decode")
        .description("Select whether to encode or decode the text.")
        .options(
            option("Encode", OperationType.ENCODE.name()),
            option("Decode", OperationType.DECODE.name()))
        .required(true);

    public static final ModifiableStringProperty TEXT_PROPERTY = string(TEXT)
        .label("Text")
        .controlType(ControlType.TEXT_AREA)
        .required(true);
}
