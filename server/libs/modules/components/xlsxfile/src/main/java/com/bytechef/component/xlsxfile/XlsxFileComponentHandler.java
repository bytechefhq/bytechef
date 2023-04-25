
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.xlsxfile;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;

import com.bytechef.component.xlsxfile.action.XlsxFileReadAction;
import com.bytechef.component.xlsxfile.action.XlsxFileWriteAction;
import com.bytechef.component.xlsxfile.constant.XlsxFileConstants;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class XlsxFileComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(XlsxFileConstants.XLSX_FILE)
        .title("XLSX File")
        .description("Reads and writes data from a XLS/XLSX file.")
        .actions(
            XlsxFileReadAction.ACTION_DEFINITION,
            XlsxFileWriteAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
