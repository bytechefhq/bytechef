
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

import com.bytechef.hermes.component.TriggerContext;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
        HYBRID_DYNAMIC,
        HYBRID_STATIC,
        LISTENER,
        POLLING,
        WEBHOOK_DYNAMIC,
        WEBHOOK_STATIC
    }

    enum WebhookMethod {
        DELETE,
        GET,
        HEAD,
        PATCH,
        POST,
        PUT,
    }

    /**
     *
     * @return
     */
    Optional<Boolean> getBatch();

    /**
     *
     * @return
     */
    String getComponentName();

    /**
     *
     * @return
     */
    String getDescription();

    /**
     *
     * @return
     */
    Optional<DeduplicateFunction> getDeduplicate();

    /**
     *
     * @return
     */
    Optional<DynamicWebhookDisableConsumer> getDynamicWebhookDisable();

    /**
     *
     * @return
     */
    Optional<DynamicWebhookEnableFunction> getDynamicWebhookEnable();

    /**
     *
     * @return
     */
    Optional<DynamicWebhookRefreshFunction> getDynamicWebhookRefresh();

    /**
     *
     * @return
     */
    Optional<DynamicWebhookRequestFunction> getDynamicWebhookRequest();

    /**
     *
     * @return
     */
    Object getExampleOutput();

    /**
     *
     * @return
     */
    Optional<ExampleOutputDataSource> getExampleOutputDataSource();

    /**
     *
     * @return
     */
    Optional<Help> getHelp();

    /**
     *
     * @return
     */
    String getName();

    /**
     *
     * @return
     */
    NodeDescriptionFunction getNodeDescription();

    /**
     *
     * @return
     */
    Optional<ListenerEnableConsumer> getListenerEnable();

    /**
     *
     * @return
     */
    Optional<ListenerDisableConsumer> getListenerDisable();

    /**
     *
     * @return
     */
    Optional<List<? extends Property<?>>> getOutputSchema();

    /**
     *
     * @return
     */
    Optional<OutputSchemaDataSource> getOutputSchemaDataSource();

    /**
     *
     * @return
     */
    Optional<PollFunction> getPoll();

    /**
     *
     * @return
     */
    Optional<List<? extends Property<?>>> getProperties();

    /**
     *
     * @return
     */
    Optional<StaticWebhookRequestFunction> getStaticWebhookRequest();

    String getTitle();

    /**
     *
     * @return
     */
    TriggerType getType();

    /**
     *
     * @return
     */
    Optional<WebhookValidateFunction> getWebhookValidate();

    /**
     *
     * @return
     */
    Optional<Boolean> getWebhookBodyRaw();

    /**
     *
     * @return
     */
    Optional<Boolean> getWorkflowSyncExecution();

    /**
     *
     */
    interface DeduplicateFunction {

        /**
         *
         * @param record
         * @return
         */
        String apply(Map<String, Object> record);
    }

    /**
     *
     */
    @FunctionalInterface
    interface DynamicWebhookDisableConsumer {

        /**
         *
         * @param context
         */
        void accept(DynamicWebhookDisableContext context);

    }

    /**
     *
     * @param connection
     * @param inputParameters
     * @param dynamicWebhookEnableOutput
     * @param workflowExecutionId
     */
    record DynamicWebhookDisableContext(
        Connection connection, InputParameters inputParameters, DynamicWebhookEnableOutput dynamicWebhookEnableOutput,
        String workflowExecutionId) {
    }

    /**
     *
     * @param connection
     * @param inputParameters
     * @param webhookUrl
     * @param workflowExecutionId
     */
    record DynamicWebhookEnableContext(
        Connection connection, InputParameters inputParameters, String webhookUrl, String workflowExecutionId) {
    }

    /**
     *
     */
    @FunctionalInterface
    interface DynamicWebhookEnableFunction {

        /**
         * @param context
         */
        DynamicWebhookEnableOutput apply(DynamicWebhookEnableContext context);

    }

    /**
     *
     * @param parameters
     * @param webhookExpirationDate
     */
    @SuppressFBWarnings("EI")
    record DynamicWebhookEnableOutput(Map<String, Object> parameters, LocalDateTime webhookExpirationDate) {

        public Object getParameter(String key) {
            return parameters.get(key);
        }
    }

    /**
     *
     */
    @FunctionalInterface
    interface DynamicWebhookRefreshFunction {

        DynamicWebhookEnableOutput apply(DynamicWebhookEnableOutput dynamicWebhookEnableOutput);
    }

    /**
     *
     * @param triggerContext
     * @param inputParameters
     * @param headers
     * @param parameters
     * @param body
     * @param path
     * @param method
     * @param dynamicWebhookEnableOutput
     */
    record DynamicWebhookRequestContext(
        TriggerContext triggerContext, InputParameters inputParameters, WebhookHeaders headers,
        WebhookParameters parameters, WebhookBody body, String path, WebhookMethod method,
        DynamicWebhookEnableOutput dynamicWebhookEnableOutput) {
    }

    /**
     *
     */
    @FunctionalInterface
    interface DynamicWebhookRequestFunction {

        /**
         *
         * @param context
         * @return
         */
        WebhookOutput apply(DynamicWebhookRequestContext context);

    }

    /**
     *
     */
    @FunctionalInterface
    interface ListenerEnableConsumer {

        /**
         * @param connection
         * @param inputParameters
         * @param workflowExecutionId
         */
        void accept(Connection connection, InputParameters inputParameters, String workflowExecutionId);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ListenerDisableConsumer {

        /**
         *
         * @param connection
         * @param inputParameters
         */
        void accept(Connection connection, InputParameters inputParameters, String workflowExecutionId);
    }

    /**
     *
     * @param triggerContext
     * @param inputParameters
     * @param closureParameters
     */
    @SuppressFBWarnings("EI")
    record PollContext(
        TriggerContext triggerContext, InputParameters inputParameters, Map<String, Object> closureParameters) {
    }

    /**
     *
     */
    @FunctionalInterface
    interface PollFunction {

        /**
         *
         * @param context
         * @return
         */
        PollOutput apply(PollContext context);

    }

    /**
     *
     * @param records
     * @param closureParameters
     * @param pollImmediately
     */
    @SuppressFBWarnings("EI")
    record PollOutput(
        List<Map<?, ?>> records, Map<String, Object> closureParameters, boolean pollImmediately)
        implements TriggerOutput {

        @Override
        public Object getValue() {
            return records;
        }
    }

    /**
     *
     * @param triggerContext
     * @param inputParameters
     * @param headers
     * @param parameters
     * @param body
     * @param path
     * @param method
     */
    record StaticWebhookRequestContext(
        TriggerContext triggerContext, InputParameters inputParameters, WebhookHeaders headers,
        WebhookParameters parameters, WebhookBody body, String path, WebhookMethod method) {
    }

    /**
     *
     */
    @FunctionalInterface
    interface StaticWebhookRequestFunction {

        /**
         *
         * @param context
         * @return
         */
        WebhookOutput apply(StaticWebhookRequestContext context);

    }

    /**
     *
     */
    sealed interface TriggerOutput permits WebhookOutput, PollOutput {

        /**
         *
         * @return
         */
        Object getValue();
    }

    /**
     *
     */
    interface WebhookHeaders {

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
    interface WebhookBody {

        enum ContentType {
            BINARY,
            FORM_DATA,
            FORM_URL_ENCODED,
            JSON,
            RAW,
            XML
        }

        /**
         *
         * @return
         */
        Object getContent();

        /**
         *
         * @return
         */
        ContentType getContentType();

        /**
         *
         * @return
         */
        String getMimeType();
    }

    /**
     *
     */
    sealed interface WebhookOutput extends TriggerOutput
        permits WebhookOutput.ListOutput, WebhookOutput.MapOutput, WebhookOutput.RawOutput {

        default String getMessage() {
            return "Successful operation.";
        }

        default int getStatusCode() {
            return 200;
        }

        @Override
        Object getValue();

        /**
         *
         * @param records
         * @return
         */
        static WebhookOutput list(List<Map<?, ?>> records) {
            return new ListOutput(records);
        }

        /**
         *
         * @param record
         * @return
         */
        static WebhookOutput map(Map<?, ?> record) {
            return new MapOutput(record);
        }

        /**
         *
         * @param raw
         * @return
         */
        static WebhookOutput raw(String raw) {
            return new RawOutput(raw);
        }

        /**
         *
         * @param records
         */
        @SuppressFBWarnings("EI")
        record ListOutput(List<Map<?, ?>> records) implements WebhookOutput {

            @Override
            public Object getValue() {
                return records;
            }
        }

        /**
         *
         * @param record
         */
        @SuppressFBWarnings("EI")
        record MapOutput(Map<?, ?> record) implements WebhookOutput {

            @Override
            public Object getValue() {
                return record;
            }
        }

        /**
         *
         * @param raw
         */
        @SuppressFBWarnings("EI")
        record RawOutput(String raw) implements WebhookOutput {

            @Override
            public Object getValue() {
                return raw;
            }
        }
    }

    /**
     *
     */
    interface WebhookParameters {

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
     * @param triggerContext
     * @param inputParameters
     * @param headers
     * @param parameters
     * @param body
     * @param path
     * @param method
     */
    record WebhookValidateContext(
        TriggerContext triggerContext, InputParameters inputParameters, WebhookHeaders headers,
        WebhookParameters parameters, WebhookBody body, String path, WebhookMethod method) {
    }

    /**
     *
     */
    @FunctionalInterface
    interface WebhookValidateFunction {

        /**
         *
         * @param context
         * @return
         */
        boolean apply(WebhookValidateContext context);

    }
}
