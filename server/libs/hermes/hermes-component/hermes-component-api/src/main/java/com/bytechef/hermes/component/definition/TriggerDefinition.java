
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@JsonDeserialize(as = ComponentDSL.ModifiableTriggerDefinition.class)
public interface TriggerDefinition {

    enum TriggerType {
        DYNAMIC_WEBHOOK,
        HYBRID,
        MANUAL,
        POLLING,
        STATIC_WEBHOOK
    }

    Boolean getBatch();

    String getComponentName();

    Display getDisplay();

    Object getExampleOutput();

    ExampleOutputDataSource getExampleOutputDataSource();

    String getName();

    List<? extends Property<?>> getOutputSchema();

    OutputSchemaDataSource getOutputSchemaDataSource();

    List<? extends Property<?>> getProperties();

    Resources getResources();

    TriggerType getType();

    Optional<ManualEnableConsumer> getManualEnable();

    Optional<ManualDisableConsumer> getManualDisable();

    Optional<PollDisableConsumer> getPollDisable();

    Optional<PollEnableConsumer> getPollEnable();

    Optional<PollFunction> getPoll();

    Optional<WebhookDisableConsumer> getWebhookDisable();

    Optional<WebhookEnableFunction> getWebhookEnable();

    Optional<WebhookRefreshFunction> getWebhookRefresh();

    Optional<WebhookRequestFunction> getWebhookRequest();

    /**
     *
     */
    interface Emitter {

        /**
         *
         * @param parameters
         */
        void emit(Map<String, Object> parameters);
    }

    /**
     *
     */
    interface WebhookHeaderParameters {

        /**
         *
         * @param name
         * @return
         */
        String getValue(String name);

        /**
         *
         * @param name
         * @return
         */
        String[] getValues(String name);
    }

    /**
     *
     */
    interface WebhookPayload {

        /**
         *
         * @return
         */
        Object getValue();
    }

    /**
     *
     */
    interface WebhookQueryParameters {

        /**
         *
         * @param name
         * @return
         */
        String getValue(String name);

        /**
         *
         * @param name
         * @return
         */
        String[] getValues(String name);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ManualEnableConsumer {

        /**
         *
         * @param connection
         * @param inputParameters
         * @param emitter
         */
        void accept(Connection connection, InputParameters inputParameters, Emitter emitter);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ManualDisableConsumer {

        /**
         *
         * @param connection
         * @param inputParameters
         */
        void accept(Connection connection, InputParameters inputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface PollDisableConsumer {

        /**
         *
         * @param connection
         * @param inputParameters
         */
        void accept(Connection connection, InputParameters inputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface PollEnableConsumer {

        /**
         *
         * @param connection
         * @param inputParameters
         */
        void accept(Connection connection, InputParameters inputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface PollFunction {

        PollOutput apply(Context context, InputParameters inputParameters, Map<String, Object> closureParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface WebhookDisableConsumer {

        /**
         *
         * @param connection
         * @param inputParameters
         */
        void accept(Connection connection, InputParameters inputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface WebhookEnableFunction {

        /**
         *
         * @param connection
         * @param inputParameters
         */
        WebhookEnableOutput apply(Connection connection, InputParameters inputParameters, String webhookUrl);
    }

    /**
     *
     */
    @FunctionalInterface
    interface WebhookRequestFunction {

        /**
         *
         * @param context
         * @param inputParameters
         * @param headerParameters
         * @param queryParameters
         * @param payload
         * @return
         */
        Object apply(
            Context context, InputParameters inputParameters, WebhookHeaderParameters headerParameters,
            WebhookQueryParameters queryParameters, WebhookPayload payload);
    }

    /**
     *
     */
    @FunctionalInterface
    interface WebhookRefreshFunction {

        WebhookEnableOutput apply(WebhookEnableOutput webhookEnableOutput);
    }

    /**
     *
     * @param result
     * @param closureParameters
     * @param pollImmediately
     */
    record PollOutput(Object result, Map<String, Object> closureParameters, boolean pollImmediately) {
    }

    /**
     *
     * @param parameters
     * @param webhookExpirationDate
     */
    record WebhookEnableOutput(Map<String, Object> parameters, LocalDateTime webhookExpirationDate) {
    }
}
