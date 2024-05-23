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

package com.bytechef.component.xml.helper;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.xml.helper.constant.XmlHelperConstants.XML_HELPER;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.xml.helper.action.XmlHelperParseAction;
import com.bytechef.component.xml.helper.action.XmlHelperStringifyAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class XmlHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(XML_HELPER)
        .title("XML Helper")
        .description("Converts between XML string and object/array.")
        .icon("path:assets/xml-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            XmlHelperParseAction.ACTION_DEFINITION,
            XmlHelperStringifyAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
