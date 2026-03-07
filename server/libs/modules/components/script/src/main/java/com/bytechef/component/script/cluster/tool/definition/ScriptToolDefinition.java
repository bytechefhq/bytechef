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

package com.bytechef.component.script.cluster.tool.definition;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.component.script.engine.PolyglotEngine;
import com.bytechef.platform.component.definition.AbstractClusterElementDefinitionWrapper;
import com.bytechef.platform.component.definition.ClusterElementContextAware;

/**
 * @author Ivica Cardic
 */
public class ScriptToolDefinition
    extends AbstractClusterElementDefinitionWrapper<ScriptToolDefinition>
    implements ToolFunction {

    private final String languageId;
    private final PolyglotEngine polyglotEngine;

    public ScriptToolDefinition(
        ClusterElementDefinition<?> clusterElementDefinition, String languageId, PolyglotEngine polyglotEngine) {

        super(clusterElementDefinition);

        this.languageId = languageId;
        this.polyglotEngine = polyglotEngine;
    }

    @Override
    public Object apply(Parameters inputParameters, Parameters connectionParameters, ClusterElementContext context)
        throws Exception {

        return polyglotEngine.execute(languageId, inputParameters, null, (ClusterElementContextAware) context);
    }
}
