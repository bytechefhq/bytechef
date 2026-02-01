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

package com.bytechef.component.script.datastream.definition;

import static com.bytechef.component.script.constant.ScriptConstants.INPUT;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.script.engine.PolyglotEngine;
import com.bytechef.platform.component.definition.AbstractClusterElementDefinitionWrapper;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.datastream.ItemProcessor;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ScriptClusterElementDefinition
    extends AbstractClusterElementDefinitionWrapper<ScriptClusterElementDefinition>
    implements ItemProcessor<Object, Object> {

    private final String languageId;
    private final PolyglotEngine polyglotEngine;

    public ScriptClusterElementDefinition(
        ClusterElementDefinition<?> clusterElementDefinition, String languageId, PolyglotEngine polyglotEngine) {

        super(clusterElementDefinition);

        this.languageId = languageId;
        this.polyglotEngine = polyglotEngine;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> process(
        Map<String, Object> item, Parameters inputParameters, Parameters connectionParameters,
        ClusterElementContext context) {

        return (Map<String, Object>) polyglotEngine.execute(
            languageId, ParametersFactory.create(MapUtils.concat(inputParameters, Map.of(INPUT, Map.of("item", item)))),
            null, (ClusterElementContextAware) context);
    }
}
