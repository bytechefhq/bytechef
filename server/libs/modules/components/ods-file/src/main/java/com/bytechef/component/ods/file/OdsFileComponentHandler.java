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

package com.bytechef.component.ods.file;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;

import com.bytechef.component.ods.file.action.OdsFileReadAction;
import com.bytechef.component.ods.file.action.OdsFileWriteAction;
import com.bytechef.component.ods.file.constant.OdsFileConstants;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
@AutoService(ComponentHandler.class)
public class OdsFileComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(OdsFileConstants.ODS_FILE)
        .title("ODS File")
        .description("Reads and writes data from a ODS file.")
        .icon("path:assets/ods-file.svg")
        .actions(
            OdsFileReadAction.ACTION_DEFINITION,
            OdsFileWriteAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
