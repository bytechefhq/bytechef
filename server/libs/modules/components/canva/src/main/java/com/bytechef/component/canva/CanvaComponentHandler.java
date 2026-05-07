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

package com.bytechef.component.canva;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.canva.action.CanvaCreateDesignAction;
import com.bytechef.component.canva.action.CanvaExportDesignAction;
import com.bytechef.component.canva.action.CanvaUploadAssetAction;
import com.bytechef.component.canva.connection.CanvaConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivona Pavela
 */
@AutoService(ComponentHandler.class)
public class CanvaComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("canva")
        .title("Canva")
        .description(
            "Canva is a web and mobile application designed to help users create, design, and collaborate on visual content.")
        .customAction(true)
        .customActionHelp("Canva API documentation", "https://www.canva.dev/docs/connect/")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(CanvaConnection.CONNECTION_DEFINITION)
        .actions(
            CanvaCreateDesignAction.ACTION_DEFINITION,
            CanvaExportDesignAction.ACTION_DEFINITION,
            CanvaUploadAssetAction.ACTION_DEFINITION)
        .icon("path:assets/canva.svg")
        .clusterElements(
            tool(CanvaCreateDesignAction.ACTION_DEFINITION),
            tool(CanvaExportDesignAction.ACTION_DEFINITION),
            tool(CanvaUploadAssetAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
