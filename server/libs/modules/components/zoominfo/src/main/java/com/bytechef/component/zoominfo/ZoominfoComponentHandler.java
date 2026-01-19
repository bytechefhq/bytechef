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

package com.bytechef.component.zoominfo;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zoominfo.action.ZoominfoEnrichCompanyAction;
import com.bytechef.component.zoominfo.action.ZoominfoEnrichContactAction;
import com.bytechef.component.zoominfo.action.ZoominfoSearchCompanyAction;
import com.bytechef.component.zoominfo.action.ZoominfoSearchContactAction;
import com.bytechef.component.zoominfo.connection.ZoominfoConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class ZoominfoComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("zoominfo")
        .title("ZoomInfo")
        .description(
            "ZoomInfo is a platform that provides companies with accurate contact data and sales insights to help them find and engage potential customers.")
        .icon("path:assets/zoominfo.svg")
        .categories(ComponentCategory.MARKETING_AUTOMATION)
        .connection(ZoominfoConnection.CONNECTION_DEFINITION)
        .clusterElements(
            tool(ZoominfoEnrichCompanyAction.ACTION_DEFINITION),
            tool(ZoominfoEnrichContactAction.ACTION_DEFINITION),
            tool(ZoominfoSearchCompanyAction.ACTION_DEFINITION),
            tool(ZoominfoSearchContactAction.ACTION_DEFINITION))
        .actions(
            ZoominfoEnrichCompanyAction.ACTION_DEFINITION,
            ZoominfoEnrichContactAction.ACTION_DEFINITION,
            ZoominfoSearchCompanyAction.ACTION_DEFINITION,
            ZoominfoSearchContactAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
