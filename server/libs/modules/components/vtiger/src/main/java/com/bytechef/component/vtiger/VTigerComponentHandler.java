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

package com.bytechef.component.vtiger;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.vtiger.constant.VTigerConstants.VTIGER;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.vtiger.action.VTigerCreateContactAction;
import com.bytechef.component.vtiger.action.VTigerCreateProductAction;
import com.bytechef.component.vtiger.action.VTigerGetMeAction;
import com.bytechef.component.vtiger.connection.VTigerConnection;
import com.google.auto.service.AutoService;

/**
 * @author Luka Ljubić
 */
@AutoService(ComponentHandler.class)
public class VTigerComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(VTIGER)
        .title("VTiger")
        .description("CRM software for sales, marketing, and support teams")
        .icon("path:assets/vtiger.svg")
        .categories(ComponentCategory.CRM)
        .connection(VTigerConnection.CONNECTION_DEFINITION)
        .actions(
            VTigerCreateContactAction.ACTION_DEFINITION,
            VTigerCreateProductAction.ACTION_DEFINITION,
            VTigerGetMeAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
