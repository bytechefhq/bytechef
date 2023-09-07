
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.registry.service;

import com.bytechef.hermes.component.definition.TriggerDefinition;

import java.util.Map;

public record StaticWebhookRequestContextImpl(
    Map<String, ?> inputParameters, TriggerDefinition.HttpHeaders headers, TriggerDefinition.HttpParameters parameters,
    TriggerDefinition.WebhookBody body,
    TriggerDefinition.WebhookMethod method, TriggerDefinition.TriggerContext triggerContext)
    implements TriggerDefinition.StaticWebhookRequestContext {
}
