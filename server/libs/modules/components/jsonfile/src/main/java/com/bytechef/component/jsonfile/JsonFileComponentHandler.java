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

package com.bytechef.component.jsonfile;

import static com.bytechef.component.jsonfile.constant.JsonFileConstants.JSON_FILE;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;

import com.bytechef.component.jsonfile.action.JsonFileReadAction;
import com.bytechef.component.jsonfile.action.JsonFileWriteAction;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class JsonFileComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(JSON_FILE)
        .title("JSON File")
        .description("Reads and writes data from a JSON file.")
        .icon("path:assets/jsonfile.svg")
        .actions(
            JsonFileReadAction.ACTION_DEFINITION,
            JsonFileWriteAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
