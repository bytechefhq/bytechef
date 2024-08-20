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

package com.bytechef.component.zhipu;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zhipu.action.ZhiPuChatAction;
import com.bytechef.component.zhipu.action.ZhiPuCreateImageAction;
import com.bytechef.component.zhipu.connection.ZhiPuConnection;
import com.bytechef.component.zhipu.constant.ZhiPuConstants;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class ZhiPuComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(ZhiPuConstants.ZHIPU)
        .title("ZhiPu AI")
        .description(
            "Zhipu AI is an artificial intelligence company with the mission of teaching machines to think like humans.")
        .icon("path:assets/zhipu.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(ZhiPuConnection.CONNECTION_DEFINITION)
        .actions(ZhiPuChatAction.ACTION_DEFINITION, ZhiPuCreateImageAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
