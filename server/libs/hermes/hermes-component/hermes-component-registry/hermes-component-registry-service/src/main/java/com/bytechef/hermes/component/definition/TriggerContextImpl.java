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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * @author Ivica Cardic
 */
public class TriggerContextImpl extends ContextImpl implements TriggerDefinition.TriggerContext {

    public TriggerContextImpl(
        String componentName, String triggerName, ComponentConnection connection, HttpClientExecutor httpClientExecutor,
        ObjectMapper objectMapper, XmlMapper xmlMapper) {

        super(componentName, triggerName, connection, httpClientExecutor, objectMapper, xmlMapper);
    }
}
