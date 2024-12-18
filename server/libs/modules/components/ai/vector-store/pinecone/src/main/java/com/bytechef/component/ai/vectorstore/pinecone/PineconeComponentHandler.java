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

package com.bytechef.component.ai.vectorstore.pinecone;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.pinecone.action.PineconeDataQueryAction;
import com.bytechef.component.ai.vectorstore.pinecone.action.PineconeLoadDataAction;
import com.bytechef.component.ai.vectorstore.pinecone.connection.PineconeConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class PineconeComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("pinecone")
        .title("Pinecone")
        .description(
            "Pinecone is a vector database designed for efficient similarity search and storage of high-dimensional " +
                "data, commonly used in machine learning and AI applications.")
        .icon("path:assets/pinecone.svg")
        .connection(PineconeConnection.CONNECTION_DEFINITION)
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .actions(
            PineconeDataQueryAction.ACTION_DEFINITION,
            PineconeLoadDataAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
