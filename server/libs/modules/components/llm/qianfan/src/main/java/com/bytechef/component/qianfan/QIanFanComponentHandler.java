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

package com.bytechef.component.qianfan;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.qianfan.action.QIanFanChatAction;
import com.bytechef.component.qianfan.action.QIanFanCreateImageAction;
import com.bytechef.component.qianfan.connection.QIanFanConnection;
import com.bytechef.component.qianfan.constant.QIanFanConstants;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class QIanFanComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(QIanFanConstants.QIANFAN)
        .title("QIanFan")
        .description(
            "百度智能云 云智一体深入产业. 全栈自研的AI大底座，满足产业对智算基础设施的需求.")
        .icon("path:assets/qianfan.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(QIanFanConnection.CONNECTION_DEFINITION)
        .actions(QIanFanChatAction.ACTION_DEFINITION, QIanFanCreateImageAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
