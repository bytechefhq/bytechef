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

package com.bytechef.component.approval.task.action;

import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.DEFAULT_VALUE;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.FIELD_DESCRIPTION;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.FIELD_LABEL;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.FIELD_NAME;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.FIELD_OPTIONS;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.FIELD_TYPE;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.FORM_DESCRIPTION;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.FORM_TITLE;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.INPUTS;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.MAX_SELECTION;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.MIN_SELECTION;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.MULTIPLE_CHOICE;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.PLACEHOLDER;
import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.REQUIRED;
import static com.bytechef.component.approval.task.util.FieldType.CHECKBOX;
import static com.bytechef.component.approval.task.util.FieldType.CUSTOM_HTML;
import static com.bytechef.component.approval.task.util.FieldType.DATETIME_PICKER;
import static com.bytechef.component.approval.task.util.FieldType.DATE_PICKER;
import static com.bytechef.component.approval.task.util.FieldType.EMAIL_INPUT;
import static com.bytechef.component.approval.task.util.FieldType.FILE_INPUT;
import static com.bytechef.component.approval.task.util.FieldType.HIDDEN_FIELD;
import static com.bytechef.component.approval.task.util.FieldType.INPUT;
import static com.bytechef.component.approval.task.util.FieldType.NUMBER_INPUT;
import static com.bytechef.component.approval.task.util.FieldType.PASSWORD_INPUT;
import static com.bytechef.component.approval.task.util.FieldType.RADIO;
import static com.bytechef.component.approval.task.util.FieldType.SELECT;
import static com.bytechef.component.approval.task.util.FieldType.TEXTAREA;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;

/**
 * @author Ivica Cardic
 */
public class ApproveAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("approve")
        .title("Approve")
        .description("Sends an approval request and waits for a human to approve or reject.")
        .properties(
            string(FORM_TITLE)
                .label("Form Title")
                .description("The title for the approval form. Displayed as the main heading.")
                .required(false),
            string(FORM_DESCRIPTION)
                .label("Form Description")
                .description("A description shown under the form title. Use \\n or <br> for line breaks.")
                .controlType(TEXT_AREA)
                .required(false),
            array(INPUTS)
                .label("Form Inputs")
                .description("Define the form input fields for the approval request.")
                .items(
                    object()
                        .properties(
                            integer(FIELD_TYPE)
                                .label("Field Type")
                                .description("The type of the form field.")
                                .options(
                                    option("Checkbox", CHECKBOX.getValue()),
                                    option("Custom HTML", CUSTOM_HTML.getValue()),
                                    option("Date Picker", DATE_PICKER.getValue()),
                                    option("Datetime Picker", DATETIME_PICKER.getValue()),
                                    option("Email", EMAIL_INPUT.getValue()),
                                    option("File", FILE_INPUT.getValue()),
                                    option("Hidden Field", HIDDEN_FIELD.getValue()),
                                    option("Input", INPUT.getValue()),
                                    option("Number", NUMBER_INPUT.getValue()),
                                    option("Password", PASSWORD_INPUT.getValue()),
                                    option("Radio Button", RADIO.getValue()),
                                    option("Select", SELECT.getValue()),
                                    option("Textarea", TEXTAREA.getValue()))
                                .required(true),
                            string(FIELD_LABEL)
                                .label("Field Label")
                                .description("The label of the form field."),
                            string(FIELD_NAME)
                                .label("Field Name")
                                .description("The name of the form field.")
                                .displayCondition(
                                    "inputs[index].fieldType != %s".formatted(CUSTOM_HTML.getValue())),
                            string(FIELD_DESCRIPTION)
                                .label("Field Description")
                                .description("Description of the form field.")
                                .required(false),
                            string(PLACEHOLDER)
                                .label("Placeholder")
                                .description("The placeholder text.")
                                .required(false)
                                .displayCondition(
                                    "contains({%s,%s,%s,%s,%s}, inputs[index].fieldType)".formatted(
                                        EMAIL_INPUT.getValue(), INPUT.getValue(), NUMBER_INPUT.getValue(),
                                        PASSWORD_INPUT.getValue(), TEXTAREA.getValue())),
                            string(DEFAULT_VALUE)
                                .label("Default value")
                                .description("Pre-filled or pre-selected value for compatible fields.")
                                .displayCondition(
                                    "inputs[index].fieldType != %s".formatted(CUSTOM_HTML.getValue()))
                                .required(false),
                            string(DEFAULT_VALUE)
                                .label("Default value")
                                .description("Pre-filled or pre-selected value for compatible fields.")
                                .controlType(TEXT_AREA)
                                .displayCondition(
                                    "inputs[index].fieldType == %s".formatted(CUSTOM_HTML.getValue()))
                                .required(false),
                            array(FIELD_OPTIONS)
                                .label("Field Options")
                                .description("The field label/value options.")
                                .items(
                                    object()
                                        .properties(
                                            string("label")
                                                .label("Label")
                                                .required(true),
                                            string("value")
                                                .label("Value")
                                                .required(true)))
                                .required(false)
                                .displayCondition(
                                    "contains({%s,%s}, inputs[index].fieldType)".formatted(
                                        RADIO.getValue(), SELECT.getValue())),
                            bool(MULTIPLE_CHOICE)
                                .label("Multiple Choice")
                                .description("Allow multiple selections.")
                                .defaultValue(false)
                                .required(false)
                                .displayCondition(
                                    "inputs[index].fieldType == %s".formatted(SELECT.getValue())),
                            integer(MIN_SELECTION)
                                .label("Min selection")
                                .description("Minimum selections required.")
                                .required(false)
                                .displayCondition(
                                    "inputs[index].fieldType == %s and inputs[index].multipleChoice == true"
                                        .formatted(SELECT.getValue())),
                            integer(MAX_SELECTION)
                                .label("Max selection")
                                .description("Maximum selections allowed.")
                                .required(false)
                                .displayCondition(
                                    "inputs[index].fieldType == %s and inputs[index].multipleChoice == true"
                                        .formatted(SELECT.getValue())),
                            bool(REQUIRED)
                                .label("Required")
                                .description("Whether this field is required.")
                                .defaultValue(false)
                                .required(false)))
                .required(false));
}
