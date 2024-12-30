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

package com.bytechef.component.ai.vectorstore.pinecone.action;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.DATA_QUERY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.QUERY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.vectorstore.pinecone.constant.PineconeConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class PineconeDataQueryAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DATA_QUERY)
        .title("Data Query")
        .description("Query data from a Pinecone vector store using OpenAI embeddings.")
        .properties(
            string(QUERY)
                .label("Query")
                .description("The query to be executed.")
                .required(true))
        .output()
        .perform(PineconeDataQueryAction::perform);

    private PineconeDataQueryAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return PineconeConstants.VECTOR_STORE.query(inputParameters, connectionParameters);
    }
}
