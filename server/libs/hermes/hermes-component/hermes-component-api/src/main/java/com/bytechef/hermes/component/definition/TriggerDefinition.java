
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
import com.bytechef.hermes.definition.Property.InputProperty;
import com.bytechef.hermes.definition.Property.OutputProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinition {

    /**
     *
     * @return
     */
    enum TriggerType {
        DYNAMIC_WEBHOOK,
        HYBRID,
        LISTENER,
        POLLING,
        STATIC_WEBHOOK
    }

    /**
     *
     * @return
     */
    enum WebhookMethod {
        DELETE,
        GET,
        HEAD,
        PATCH,
        POST,
        PUT,
    }

    /**
     * TODO
     *
     * @return
     */
    Optional<Boolean> getBatch();

    Optional<String> getComponentDescription();

    String getComponentName();

    Optional<String> getComponentTitle();

    /**
     * TODO
     *
     * @return
     */
    Optional<Boolean> getDeprecated();

    /**
     *
     * @return
     */
    Optional<String> getDescription();

    /**
     * TODO
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
    Optional<EditorDescriptionDataSource> getEditorDescriptionDataSource();

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
    Optional<List<? extends OutputProperty<?>>> getOutputSchema();

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
    Optional<List<? extends InputProperty>> getProperties();

    /**
     *
     * @return
     */
    Optional<Object> getSampleOutput();

    /**
     *
     * @return
     */
    Optional<SampleOutputDataSource> getSampleOutputDataSource();

    /**
     *
     * @return
     */
    Optional<StaticWebhookRequestFunction> getStaticWebhookRequest();

    /**
     *
     * @return
     */
    Optional<String> getTitle();

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
     * TODO
     *
     * @return
     */
    Optional<Boolean> getWebhookBodyRaw();

    /**
     * TODO
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
    @SuppressFBWarnings("EI")
    record DynamicWebhookDisableContext(
        Connection connection, Map<String, ?> inputParameters, DynamicWebhookEnableOutput dynamicWebhookEnableOutput,
        String workflowExecutionId) {
    }

    /**
     *
     * @param connection
     * @param inputParameters
     * @param webhookUrl
     * @param workflowExecutionId
     */
    @SuppressFBWarnings("EI")
    record DynamicWebhookEnableContext(
        Connection connection, Map<String, ?> inputParameters, String webhookUrl, String workflowExecutionId) {
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
     * @param inputParameters
     * @param headers
     * @param parameters
     * @param body
     * @param method
     * @param dynamicWebhookEnableOutput
     * @param triggerContext
     */
    @SuppressFBWarnings("EI")
    record DynamicWebhookRequestContext(
        Map<String, ?> inputParameters, WebhookHeaders headers, WebhookParameters parameters, WebhookBody body,
        WebhookMethod method, DynamicWebhookEnableOutput dynamicWebhookEnableOutput, TriggerContext triggerContext) {
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
        void accept(Connection connection, Map<String, ?> inputParameters, String workflowExecutionId);
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
        void accept(Connection connection, Map<String, ?> inputParameters, String workflowExecutionId);
    }

    /**
     * @param inputParameters
     * @param closureParameters
     * @param triggerContext
     */
    @SuppressFBWarnings("EI")
    record PollContext(
        Map<String, ?> inputParameters, Map<String, Object> closureParameters, TriggerContext triggerContext) {
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
     * @param inputParameters
     * @param headers
     * @param parameters
     * @param body
     * @param method
     * @param triggerContext
     */
    @SuppressFBWarnings("EI")
    record StaticWebhookRequestContext(
        Map<String, ?> inputParameters, WebhookHeaders headers, WebhookParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext triggerContext) {
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
        <T> T getContent();

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
     * @param inputParameters
     * @param headers
     * @param parameters
     * @param body
     * @param method
     * @param triggerContext
     */
    @SuppressFBWarnings("EI")
    record WebhookValidateContext(
        Map<String, ?> inputParameters, WebhookHeaders headers, WebhookParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext triggerContext) {
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
