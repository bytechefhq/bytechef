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

package com.bytechef.component.mongodb;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.mongodb.action.MongoDBFindAction;
import com.bytechef.component.mongodb.action.MongoDBInsertManyAction;
import com.bytechef.component.mongodb.action.MongoDBInsertOneAction;
import com.bytechef.component.mongodb.action.MongoDBUpdateManyAction;
import com.bytechef.component.mongodb.action.MongoDBUpdateOneAction;
import com.bytechef.component.mongodb.connection.MongoDBConnection;
import com.google.auto.service.AutoService;

/**
 * @author Alex Bevilacqua
 */
@AutoService(ComponentHandler.class)
public class MongoDBComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("mongodb")
        .title("MongoDB")
        .description(
            "MongoDB is a source-available, cross-platform, document-oriented database. Query, insert, update and " +
                "delete documents in your collections.")
        .icon("path:assets/mongodb.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(MongoDBConnection.CONNECTION_DEFINITION)
        .actions(
            MongoDBFindAction.ACTION_DEFINITION,
            MongoDBInsertOneAction.ACTION_DEFINITION,
            MongoDBInsertManyAction.ACTION_DEFINITION,
            MongoDBUpdateOneAction.ACTION_DEFINITION,
            MongoDBUpdateManyAction.ACTION_DEFINITION)
        .clusterElements(
            tool(MongoDBFindAction.ACTION_DEFINITION),
            tool(MongoDBInsertOneAction.ACTION_DEFINITION),
            tool(MongoDBInsertManyAction.ACTION_DEFINITION),
            tool(MongoDBUpdateOneAction.ACTION_DEFINITION),
            tool(MongoDBUpdateManyAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
