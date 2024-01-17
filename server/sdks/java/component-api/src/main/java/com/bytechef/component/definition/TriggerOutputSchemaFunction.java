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

package com.bytechef.component.definition;

import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;

/**
 * @author Ivica Cardic
 */
public interface TriggerOutputSchemaFunction {

    /**
     *
     */
    @FunctionalInterface
    interface DynamicWebhookTriggerOutputSchemaFunction extends TriggerOutputSchemaFunction {

        /**
         * @param inputParameters
         * @param connectionParameters
         * @param headers
         * @param parameters
         * @param body
         * @param method
         * @param output
         * @param context
         * @return
         */
        OutputSchema apply(
            Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
            HttpParameters parameters, WebhookBody body, WebhookMethod method, DynamicWebhookEnableOutput output,
            TriggerContext context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface ListenerTriggerOutputSchemaFunction extends TriggerOutputSchemaFunction {

        /**
         * @param inputParameters
         * @param connectionParameters
         * @param workflowExecutionId
         */
        OutputSchema accept(
            Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
            TriggerContext context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface PollTriggerOutputSchemaFunction extends TriggerOutputSchemaFunction {

        /**
         * @param inputParameters
         * @param closureParameters
         * @param context
         * @return
         */
        OutputSchema apply(Parameters inputParameters, Parameters closureParameters, TriggerContext context)
            throws Exception;

    }

    /**
     *
     */
    @FunctionalInterface
    interface StaticWebhookTriggerOutputSchemaFunction extends TriggerOutputSchemaFunction {

        /**
         * @param inputParameters
         * @param headers
         * @param parameters
         * @param body
         * @param method
         * @param context
         * @return
         */
        OutputSchema apply(
            Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
            WebhookMethod method, TriggerContext context) throws Exception;

    }
}
