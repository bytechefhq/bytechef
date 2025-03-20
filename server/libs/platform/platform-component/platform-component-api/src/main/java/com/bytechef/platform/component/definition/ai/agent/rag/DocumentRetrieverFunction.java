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

package com.bytechef.platform.component.definition.ai.agent.rag;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import java.util.Map;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface DocumentRetrieverFunction {

    /**
     *
     */
    ClusterElementType DOCUMENT_RETRIEVER = new ClusterElementType(
        "DOCUMENT_RETRIEVER", "documentRetriever", "Document Retriever", true);

    /**
     * @param inputParameters
     * @param connectionParameters
     * @param extensions
     * @param componentConnections
     * @return
     * @throws Exception
     */
    DocumentRetriever apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception;
}
