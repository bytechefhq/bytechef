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

package com.bytechef.component.microsoft.excel.constant;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftExcelConstants {

    public static final String COLUMN = "column";
    public static final String ID = "id";
    public static final String IS_THE_FIRST_ROW_HEADER = "isTheFirstRowHeader";
    public static final String NAME = "name";
    public static final String ROW = "row";
    public static final String ROW_NUMBER = "rowNumber";
    public static final String UPDATE_WHOLE_ROW = "updateWholeRow";
    public static final String VALUE = "value";
    public static final String VALUES = "values";
    public static final String WORKBOOK_ID = "workbookId";
    public static final String WORKSHEET_NAME = "worksheetName";

    public static final ModifiableBooleanProperty IS_THE_FIRST_ROW_HEADER_PROPERTY = bool(IS_THE_FIRST_ROW_HEADER)
        .label("Is the First Row Header?")
        .description("If the first row is header.")
        .defaultValue(false)
        .required(true);

    public static final ModifiableStringProperty WORKBOOK_ID_PROPERTY = string(WORKBOOK_ID)
        .label("Workbook ID")
        .options((OptionsFunction<String>) MicrosoftExcelUtils::getWorkbookIdOptions)
        .required(true);

    public static final ModifiableStringProperty WORKSHEET_NAME_PROPERTY = string(WORKSHEET_NAME)
        .label("Worksheet")
        .options((OptionsFunction<String>) MicrosoftExcelUtils::getWorksheetNameOptions)
        .optionsLookupDependsOn(WORKBOOK_ID)
        .required(true);

    private MicrosoftExcelConstants() {
    }
}
