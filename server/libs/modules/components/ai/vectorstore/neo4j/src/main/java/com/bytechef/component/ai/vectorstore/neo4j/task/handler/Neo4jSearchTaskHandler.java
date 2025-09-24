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

package com.bytechef.component.ai.vectorstore.neo4j.task.handler;

import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.NEO4J;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.SEARCH;

import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.workflow.worker.task.handler.AbstractTaskHandler;
import org.springframework.stereotype.Component;

/**
 * @author Monika Kušter
 */
@Component(NEO4J + "/v1/" + SEARCH)
public class Neo4jSearchTaskHandler extends AbstractTaskHandler {

    public Neo4jSearchTaskHandler(ActionDefinitionFacade actionDefinitionFacade) {
        super(NEO4J, 1, SEARCH, actionDefinitionFacade);
    }
}
