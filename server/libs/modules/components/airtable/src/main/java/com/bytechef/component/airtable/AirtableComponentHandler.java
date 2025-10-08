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

package com.bytechef.component.airtable;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.airtable.datastream.AirtableItemReader;
import com.bytechef.component.airtable.datastream.AirtableItemWriter;
import com.bytechef.component.airtable.trigger.AirtableNewRecordTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.exception.ProviderException;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.definition.ComponentDsl.tool;

/**
 * @author Ivica Cardic
 */
@AutoService(OpenApiComponentHandler.class)
public class AirtableComponentHandler extends AbstractAirtableComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(AirtableNewRecordTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableActionDefinition modifyAction(ModifiableActionDefinition modifiableActionDefinition) {
        modifiableActionDefinition.processErrorResponse(
            (statusCode, body, context) -> {
                String message;

                if (body instanceof Map<?, ?> map) {
                    message = (String) ((Map<?, ?>) map.get("error")).get("message");
                } else {
                    message = body == null ? null : body.toString();
                }

                return new ProviderException(statusCode, message);
            });

        return modifiableActionDefinition;
    }

    @Override
    public List<ModifiableClusterElementDefinition<?>> getCustomClusterElements() {
        return List.of(
            AirtableItemReader.CLUSTER_ELEMENT_DEFINITION,
            AirtableItemWriter.CLUSTER_ELEMENT_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/airtable.svg")
            .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION);
    }
}
