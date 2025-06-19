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

package com.bytechef.component.jwt.helper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.jwt.helper.action.JwtHelperSignAction;
import com.bytechef.component.jwt.helper.action.JwtHelperVerifyAction;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class JwtHelperComponentHandler implements ComponentHandler {
    private static final ComponentDefinition COMPONENT_DEFINITION = component("jwtHelper")
        .title("JWT Helper")
        .description("JWT helper component provides actions for signing and verifying JWT tokens.")
        .icon("path:assets/jwt-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            JwtHelperSignAction.ACTION_DEFINITION,
            JwtHelperVerifyAction.ACTION_DEFINITION)
        .clusterElements(
            tool(JwtHelperSignAction.ACTION_DEFINITION),
            tool(JwtHelperVerifyAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
