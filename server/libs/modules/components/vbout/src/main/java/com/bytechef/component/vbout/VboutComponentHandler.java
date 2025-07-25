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

package com.bytechef.component.vbout;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.vbout.action.VboutAddContactToListAction;
import com.bytechef.component.vbout.action.VboutAddTagToContactAction;
import com.bytechef.component.vbout.action.VboutCreateEmailListAction;
import com.bytechef.component.vbout.action.VboutCreateEmailMarketingCampaignAction;
import com.bytechef.component.vbout.action.VboutCreateSocialMediaMessageAction;
import com.bytechef.component.vbout.action.VboutUpdateContactAction;
import com.bytechef.component.vbout.connection.VboutConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class VboutComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("vbout")
        .title("Vbout")
        .description(
            "VBOUT is an AI-enabled marketing platform that helps businesses manage and streamline their digital marketing efforts.")
        .icon("path:assets/vbout.svg")
        .customAction(true)
        .categories(ComponentCategory.MARKETING_AUTOMATION)
        .connection(VboutConnection.CONNECTION_DEFINITION)
        .actions(
            VboutAddContactToListAction.ACTION_DEFINITION,
            VboutUpdateContactAction.ACTION_DEFINITION,
            VboutCreateEmailMarketingCampaignAction.ACTION_DEFINITION,
            VboutAddTagToContactAction.ACTION_DEFINITION,
            VboutCreateEmailListAction.ACTION_DEFINITION,
            VboutCreateSocialMediaMessageAction.ACTION_DEFINITION)
        .clusterElements(
            tool(VboutAddContactToListAction.ACTION_DEFINITION),
            tool(VboutUpdateContactAction.ACTION_DEFINITION),
            tool(VboutCreateEmailMarketingCampaignAction.ACTION_DEFINITION),
            tool(VboutAddTagToContactAction.ACTION_DEFINITION),
            tool(VboutCreateEmailListAction.ACTION_DEFINITION),
            tool(VboutCreateSocialMediaMessageAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
