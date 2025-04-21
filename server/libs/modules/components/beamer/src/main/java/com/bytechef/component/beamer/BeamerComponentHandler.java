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

package com.bytechef.component.beamer;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.beamer.action.BeamerCreateFeatureRequestAction;
import com.bytechef.component.beamer.action.BeamerCreatePostAction;
import com.bytechef.component.beamer.action.BeamerGetFeedAction;
import com.bytechef.component.beamer.action.BeamerNewCommentAction;
import com.bytechef.component.beamer.action.BeamerNewVoteAction;
import com.bytechef.component.beamer.connection.BeamerConnection;
import com.bytechef.component.beamer.trigger.BeamerNewPostTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class BeamerComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("beamer")
        .title("Beamer")
        .description(
            "Beamer is a customer engagement platform that helps businesses communicate updates, collect feedback, " +
                "and boost user engagement through in-app notifications, changelogs, and announcements.")
        .icon("path:assets/beamer.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(BeamerConnection.CONNECTION_DEFINITION)
        .actions(
            BeamerCreateFeatureRequestAction.ACTION_DEFINITION,
            BeamerCreatePostAction.ACTION_DEFINITION,
            BeamerGetFeedAction.ACTION_DEFINITION,
            BeamerNewCommentAction.ACTION_DEFINITION,
            BeamerNewVoteAction.ACTION_DEFINITION)
        .triggers(BeamerNewPostTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
