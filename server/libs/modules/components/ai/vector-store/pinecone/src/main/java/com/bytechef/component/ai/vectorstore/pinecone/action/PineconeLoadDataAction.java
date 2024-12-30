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

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.DOCUMENT_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.DOCUMENT_TYPE_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.JSON_KEYS_TO_USE_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.KEYWORD_METADATA_ENRICHER_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.LOAD_DATA;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.SUMMARY_METADATA_ENRICHER_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.TOKEN_TEXT_SPLITTER_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.USE_KEYWORD_ENRICHER_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.USE_SUMMARY_ENRICHER_PROPERTy;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.USE_TOKEN_TEXT_SPLITTER_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.ai.vectorstore.pinecone.constant.PineconeConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class PineconeLoadDataAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LOAD_DATA)
        .title("Load Data")
        .description("Loads data into a Pinecone vector store using OpenAI embeddings.")
        .properties(
            DOCUMENT_TYPE_PROPERTY,
            JSON_KEYS_TO_USE_PROPERTY,
            DOCUMENT_PROPERTY,
            USE_TOKEN_TEXT_SPLITTER_PROPERTY,
            TOKEN_TEXT_SPLITTER_PROPERTY,
            USE_KEYWORD_ENRICHER_PROPERTY,
            KEYWORD_METADATA_ENRICHER_PROPERTY,
            USE_SUMMARY_ENRICHER_PROPERTy,
            SUMMARY_METADATA_ENRICHER_PROPERTY)
        .perform(PineconeLoadDataAction::perform);

    private PineconeLoadDataAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        PineconeConstants.VECTOR_STORE.loadData(inputParameters, connectionParameters, actionContext);

        return null;
    }
}
