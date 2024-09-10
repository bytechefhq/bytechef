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

package com.bytechef.component.script.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.script.engine.PolyglotEngine;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParameterConnection;
import java.util.Map;
import java.util.Optional;

public class ScriptActionDefinition extends AbstractActionDefinitionWrapper {

    private final String languageId;
    private final PolyglotEngine polyglotEngine;

    public ScriptActionDefinition(ActionDefinition actionDefinition, String languageId, PolyglotEngine polyglotEngine) {
        super(actionDefinition);

        this.languageId = languageId;
        this.polyglotEngine = polyglotEngine;
    }

    @Override
    public Optional<PerformFunction> getPerform() {
        return Optional.of((MultipleConnectionsPerformFunction) this::perform);
    }

    protected Object perform(
        Parameters inputParameters, Map<String, ? extends ParameterConnection> connectionParameters,
        Parameters extensions, ActionContext actionContext) {

        return polyglotEngine.execute(languageId, inputParameters, connectionParameters, actionContext);
    }
}
