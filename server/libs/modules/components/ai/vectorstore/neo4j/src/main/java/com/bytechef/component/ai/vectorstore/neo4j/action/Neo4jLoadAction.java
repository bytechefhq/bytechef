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

package com.bytechef.component.ai.vectorstore.neo4j.action;

import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.NEO4J;

import com.bytechef.component.ai.vectorstore.action.AbstractLoadAction;
import com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class Neo4jLoadAction extends AbstractLoadAction {

    public Neo4jLoadAction(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        super(NEO4J, Neo4jConstants.VECTOR_STORE, List.of(), clusterElementDefinitionService);
    }
}
