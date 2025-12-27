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

package com.bytechef.component.form.constant;

/**
 * @author Ivica Cardic
 */
public class FormConstants {

    public static final String APPEND_ATTRIBUTION = "appendAttribution";
    public static final String BUTTON_LABEL = "buttonLabel";
    public static final String CUSTOM_FORM_STYLING = "customFormStyling";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String FIELD_DESCRIPTION = "fieldDescription";
    public static final String FIELD_LABEL = "fieldLabel";
    public static final String FIELD_NAME = "fieldName";
    public static final String FIELD_OPTIONS = "fieldOptions";
    public static final String FIELD_TYPE = "fieldType";
    public static final String FORM = "form";
    public static final String FORM_DESCRIPTION = "formDescription";
//    public static final String FORM_PATH = "formPath";
    public static final String FORM_TITLE = "formTitle";
    public static final String IGNORE_BOTS = "ignoreBots";
    public static final String INPUTS = "inputs";
    public static final String MAX_SELECTION = "maxSelection";
    public static final String MIN_SELECTION = "minSelection";
    public static final String MULTIPLE_CHOICE = "multipleChoice";
    public static final String PLACEHOLDER = "placeholder";
    public static final String REQUIRED = "required";
    public static final String USE_WORKFLOW_TIMEZONE = "useWorkflowTimezone";

    public enum FieldType {
        CHECKBOX(1),
        CUSTOM_HTML(12),
        DATE_PICKER(2),
        DATETIME_PICKER(3),
        EMAIL_INPUT(8),
        FILE_INPUT(4),
        HIDDEN_FIELD(13),
        INPUT(6),
        NUMBER_INPUT(9),
        PASSWORD_INPUT(10),
        RADIO(11),
        SELECT(7),
        TEXTAREA(5);

        private final int value;

        FieldType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static FieldType valueOf(int value) {
            for (FieldType fieldType : FieldType.values()) {
                if (fieldType.value == value) {
                    return fieldType;
                }
            }

            throw new IllegalArgumentException("Invalid FieldType value: " + value);
        }
    }
}
