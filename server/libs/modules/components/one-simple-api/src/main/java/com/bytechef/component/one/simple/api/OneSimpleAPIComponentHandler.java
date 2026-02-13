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

package com.bytechef.component.one.simple.api;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.one.simple.api.action.OneSimpleAPIAddScreenshotAction;
import com.bytechef.component.one.simple.api.action.OneSimpleAPICurrencyConverterAction;
import com.bytechef.component.one.simple.api.action.OneSimpleAPIUrlShortenerAction;
import com.bytechef.component.one.simple.api.action.OneSimpleAPIWebPageInformationAction;
import com.bytechef.component.one.simple.api.connection.OneSimpleAPIConnection;
import com.google.auto.service.AutoService;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class OneSimpleAPIComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("oneSimpleAPI")
        .title("One Simple API")
        .description("A toolbox with all the things you need to get your project to success: " +
            " Image resize and CDN, PDF and Screenshots generation, Currency Exchange and Discounts, " +
            "Email Validation, QR codes, and much more!")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(OneSimpleAPIConnection.CONNECTION_DEFINITION)
        .customAction(true)
        .customActionHelp("One Simple API documentation", "https://onesimpleapi.com/docs")
        .actions(
            OneSimpleAPIAddScreenshotAction.ACTION_DEFINITION,
            OneSimpleAPICurrencyConverterAction.ACTION_DEFINITION,
            OneSimpleAPIUrlShortenerAction.ACTION_DEFINITION,
            OneSimpleAPIWebPageInformationAction.ACTION_DEFINITION)
        .clusterElements(
            tool(OneSimpleAPIAddScreenshotAction.ACTION_DEFINITION),
            tool(OneSimpleAPICurrencyConverterAction.ACTION_DEFINITION),
            tool(OneSimpleAPIUrlShortenerAction.ACTION_DEFINITION),
            tool(OneSimpleAPIWebPageInformationAction.ACTION_DEFINITION))
        .icon("path:assets/one-simple-api.svg")
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
