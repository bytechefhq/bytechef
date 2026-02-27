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

package com.bytechef.component.property.testing.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.ARRAY_DEFAULT_VALUES;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.ARRAY_MAX_ITEMS;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.ARRAY_MIN_ITEMS;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.ARRAY_NO_DEFAULT_VALUES;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.ARRAY_NO_PREDEFINED_PROPERTIES;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.ARRAY_PREDEFINED_PROPERTIES;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.BOOL;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.DATE;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.DATE_TIME;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.DISPLAY_CONDITION;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.DYNAMIC_PROPERTIES_LOOKUP;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.DYNAMIC_PROPERTY;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.FILE_ENTRY;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.INTEGER_MAX_VALUE;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.INTEGER_MIN_VALUE;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.NUMBER_MAX_PRECISION;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.NUMBER_MAX_VALUE;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.NUMBER_MIN_PRECISION;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.NUMBER_MIN_VALUE;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.NUMBER_PRECISION;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.OBJECT_DEFAULT_VALUES;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.OBJECT_NO_DEFAULT_VALUES;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.OBJECT_NO_PREDEFINED_PROPERTIES;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.OBJECT_PREDEFINED_PROPERTIES;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.OPTION_LOOKUP_DEPENDS_ON;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.OPTION_MULTISELECT;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.OPTION_NO_MULTISELECT;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.SET_FOR_OPTIONS_LOOKUP;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.STRING_MAX_LENGTH;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.STRING_MIN_LENGTH;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.STRING_REG_EX;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ActionDefinition.PropertiesFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import java.util.List;
import java.util.Map;

public class PropertyTestingAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("testingAction")
        .title("Testing")
        .description("Description")
        .properties(
            array(ARRAY_DEFAULT_VALUES)
                .label("Array With Default Values")
                .description("Default value is [element1, element2, element3]")
                .defaultValue("element1", "element2", "element3"),
            array(ARRAY_MAX_ITEMS)
                .label("Array Max Items")
                .maxItems(3)
                .description("Max items set to 3")
                .items(string()),
            array(ARRAY_MIN_ITEMS)
                .label("Array Min Items")
                .minItems(3)
                .description("Min items set to 3")
                .items(string()),
            array(ARRAY_NO_DEFAULT_VALUES)
                .label("Array With No Default Values"),
            array(ARRAY_NO_PREDEFINED_PROPERTIES)
                .label("Array No Predefined Properties"),
            array(ARRAY_PREDEFINED_PROPERTIES)
                .label("Array Predefined Properties")
                .items(
                    object()
                        .properties(
                            string("predefined1")
                                .label("Predefined String 1"),
                            string("predefined2")
                                .label("Predefined String 2"))),
            bool(BOOL)
                .label("Boolean Property and Display Condition")
                .description("Display condition string will be shown when this value is set to True."),
            date(DATE)
                .label("Date Property"),
            dateTime(DATE_TIME)
                .label("Date Time Property"),
            string(DISPLAY_CONDITION)
                .label("Display Condition")
                .description("Will be displayed only if the boolean property is true.")
                .displayCondition("%s == true".formatted(BOOL)),
            string(DYNAMIC_PROPERTIES_LOOKUP)
                .label("Dynamic Properties Lookup")
                .description("Dynamic property will be shown when this value is set."),
            dynamicProperties(DYNAMIC_PROPERTY)
                .propertiesLookupDependsOn(DYNAMIC_PROPERTIES_LOOKUP)
                .properties((PropertiesFunction) PropertyTestingAction::createDynamicParameters),
            fileEntry(FILE_ENTRY)
                .label("FileEntry Property"),
            integer(INTEGER_MAX_VALUE)
                .maxValue(10)
                .label("Integer Max Value")
                .description("Integer maximum value set to 10"),
            integer(INTEGER_MIN_VALUE)
                .minValue(10)
                .label("Integer Min Value")
                .description("Integer minimum value set to 10"),
            number(NUMBER_MAX_PRECISION)
                .label("Number Max Number Precision")
                .description("Number max number precision set to 2")
                .maxNumberPrecision(2),
            number(NUMBER_MIN_PRECISION)
                .label("Number Min Number Precision")
                .description("Number min number precision set to 2")
                .minNumberPrecision(2),
            number(NUMBER_MAX_VALUE)
                .label("Number Max Value")
                .description("Number max value set to 5")
                .maxValue(5),
            number(NUMBER_MIN_VALUE)
                .label("Number Min Value")
                .description("Number min value set to 5")
                .minValue(5),
            number(NUMBER_PRECISION)
                .label("Number Precision")
                .description("Number precision set to 3")
                .numberPrecision(3),
            object(OBJECT_DEFAULT_VALUES)
                .label("Object With Default Values")
                .description("Default value is { key1 : value1, key2: value2 }")
                .defaultValue(Map.of("key1", "value1", "key2", "value2")),
            object(OBJECT_NO_DEFAULT_VALUES)
                .label("Object With No Default Values"),
            object(OBJECT_NO_PREDEFINED_PROPERTIES)
                .label("Object No Predefined Properties"),
            object(OBJECT_PREDEFINED_PROPERTIES)
                .label("Object Predefined Properties")
                .properties(
                    array("array")
                        .items(
                            object()
                                .properties(
                                    string("key"),
                                    string("value")))),
            string(SET_FOR_OPTIONS_LOOKUP)
                .label("Set For Options Lookup")
                .description("This has to be set so you can se the options below."),
            string(OPTION_LOOKUP_DEPENDS_ON)
                .label("Option Lookup Depends On")
                .description("This options depend on \"Set For Options Lookup\" property.")
                .optionsLookupDependsOn(SET_FOR_OPTIONS_LOOKUP)
                .options((OptionsFunction<String>) PropertyTestingAction::getOptionsLookup),
            array(OPTION_MULTISELECT)
                .label("Options Multiselect")
                .items(string())
                .options(
                    List.of(
                        option("option1", "1"),
                        option("option2", "2"),
                        option("option3", "3"),
                        option("option4", "4"))),
            string(OPTION_NO_MULTISELECT)
                .label("Options No Multiselect")
                .options(
                    option("option1", "1"),
                    option("option2", "2"),
                    option("option3", "3"),
                    option("option4", "4")),
            string(STRING_MAX_LENGTH)
                .label("String Max Length")
                .maxLength(5)
                .description("Maximum length set to 5."),
            string(STRING_MIN_LENGTH)
                .label("String Min Length")
                .description("Minimum length set to 5.")
                .minLength(5),
            string(STRING_REG_EX)
                .label("String Regular Expression")
                .description(
                    "Regular expression is set to: \"[^A-Za-z]\". Just letters from a text should be returned.")
                .regex("[^A-Za-z]"))
        .output(outputSchema(object()))
        .perform(PropertyTestingAction::perform);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static List<ValueProperty<?>> createDynamicParameters(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        Context context) {

        return List.of(
            string("dynamicProperty1")
                .label("Dynamic Property 1"),
            string("dynamicProperty2")
                .label("Dynamic Property 2"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static List<? extends Option<String>> getOptionsLookup(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return List.of(
            option("option1", "1"),
            option("option2", "2"),
            option("option3", "3"),
            option("option4", "4"));
    }

    private PropertyTestingAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return inputParameters;
    }
}
