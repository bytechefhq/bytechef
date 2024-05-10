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

package com.bytechef.component.microsoft.excel.constant;

import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property.DynamicPropertiesProperty;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;

/**
 * @author Monika Domiter
 */
public class MicrosoftExcelConstants {

    public static final String APPEND_ROW = "appendRow";
    public static final String BASE_URL = "https://graph.microsoft.com/v1.0/me/drive/items";
    public static final String CLEAR_WORKSHEET = "clearWorksheet";
    public static final String DELETE_ROW = "deleteRow";
    public static final String ID = "id";
    public static final String IS_THE_FIRST_ROW_HEADER = "isTheFirstRowHeader";
    public static final String FIND_ROW_BY_NUM = "findRowByNum";
    public static final String MICROSOFT_EXCEL = "microsoftExcel";
    public static final String NAME = "name";
    public static final String ROW = "row";
    public static final String ROW_NUMBER = "rowNumber";
    public static final String TENANT_ID = "tenantId";
    public static final String UPDATE_ROW = "updateRow";
    public static final String VALUE = "value";
    public static final String VALUES = "values";
    public static final String WORKBOOK_ID = "workbookId";
    public static final String WORKSHEET_NAME = "worksheetName";
    public static final String WORKBOOK_WORKSHEETS_PATH = "/workbook/worksheets/";

    public static final ModifiableBooleanProperty IS_THE_FIRST_ROW_HEADER_PROPERTY = bool(IS_THE_FIRST_ROW_HEADER)
        .label("Is the first row header?")
        .description("If the first row is header")
        .defaultValue(false)
        .required(true);

    public static final DynamicPropertiesProperty ROW_DYNAMIC_PROPERTY = dynamicProperties(ROW)
        .propertiesLookupDependsOn(IS_THE_FIRST_ROW_HEADER, WORKSHEET_NAME, WORKBOOK_ID)
        .properties(MicrosoftExcelUtils::createInputPropertyForRow)
        .required(true);

    public static final ModifiableStringProperty WORKBOOK_ID_PROPERTY = string(WORKBOOK_ID)
        .label("Workbook")
        .options((ActionOptionsFunction<String>) MicrosoftExcelUtils::getWorkbookIdOptions)
        .required(true);

    public static final ModifiableStringProperty WORKSHEET_NAME_PROPERTY = string(WORKSHEET_NAME)
        .label("Worksheet")
        .options((ActionOptionsFunction<String>) MicrosoftExcelUtils::getWorksheetNameOptions)
        .optionsLookupDependsOn(WORKBOOK_ID)
        .required(true);

    private MicrosoftExcelConstants() {
    }
}
