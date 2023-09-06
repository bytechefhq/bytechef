
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

import com.bytechef.hermes.component.definition.Context.Connection;
import com.bytechef.hermes.definition.Help;
import com.bytechef.hermes.definition.Property.InputProperty;
import com.bytechef.hermes.definition.Property.OutputProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

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
    Optional<ListenerDisableConsumer> getListenerDisable();

    /**
     *
     * @return
     */
    Optional<ListenerEnableConsumer> getListenerEnable();

    /**
     *
     * @return
     */
    Optional<OutputProperty<?>> getOutputSchema();

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
     * TODO
     *
     * @return
     */
    Optional<Boolean> getWebhookRawBody();

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
    Optional<Boolean> getWorkflowSyncExecution();

    /**
     * TODO
     *
     * @return
     */
    Optional<Boolean> getWorkflowSyncValidation();

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
     */
    interface DynamicWebhookDisableContext {

        /**
         *
         * @return
         */
        Connection connection();

        /**
         *
         * @return
         */
        Map<String, ?> inputParameters();

        /**
         *
         * @return
         */
        DynamicWebhookEnableOutput dynamicWebhookEnableOutput();

        /**
         *
         * @return
         */
        String workflowExecutionId();
    }

    /**
     *
     */
    @FunctionalInterface
    interface DynamicWebhookEnableFunction {

        /**
         * @param context
         */
        DynamicWebhookEnableOutput apply(EnableDynamicWebhookContext context);

    }

    /**
     *
     * @param parameters
     * @param webhookExpirationDate
     */
    @SuppressFBWarnings("EI")
    record DynamicWebhookEnableOutput(Map<String, ?> parameters, LocalDateTime webhookExpirationDate) {

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
     */
    interface DynamicWebhookRequestContext {

        /**
         *
         * @return
         */
        HttpHeaders headers();

        /**
         *
         * @return
         */
        Map<String, ?> inputParameters();

        /**
         *
         * @return
         */
        HttpParameters parameters();

        /**
         *
         * @return
         */
        WebhookBody body();

        /**
         *
         * @return
         */
        WebhookMethod method();

        /**
         *
         * @return
         */
        DynamicWebhookEnableOutput dynamicWebhookEnableOutput();

        /**
         *
         * @return
         */
        TriggerContext triggerContext();
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
    interface EnableDynamicWebhookContext {

        /**
         *
         * @return
         */
        Connection connection();

        /**
         *
         * @return
         */
        Map<String, ?> inputParameters();

        /**
         *
         * @return
         */
        String webhookUrl();

        /**
         *
         * @return
         */
        String workflowExecutionId();
    }

    /**
     *
     */
    interface PollContext {

        /**
         *
         * @return
         */
        Map<String, ?> closureParameters();

        /**
         *
         * @return
         */
        Map<String, ?> inputParameters();

        /**
         *
         * @return
         */
        TriggerContext triggerContext();
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
     *
     */
    interface HttpHeaders {

        /**
         *
         * @param name
         * @return
         */
        List<String> allValues(String name);

        /**
         *
         * @param name
         * @return
         */
        Optional<String> firstValue(String name);

        /**
         *
         * @param name
         * @return
         */
        OptionalLong firstValueAsLong(String name);

        /**
         *
         * @return
         */
        Map<String, List<String>> getParameters();
    }

    /**
     *
     */
    interface HttpParameters {

        /**
         *
         * @param name
         * @return
         */
        List<String> allValues(String name);

        /**
         *
         * @param name
         * @return
         */
        Optional<String> firstValue(String name);

        /**
         *
         * @param name
         * @return
         */
        OptionalLong firstValueAsLong(String name);

        /**
         *
         * @return
         */
        Map<String, List<String>> getParameters();
    }

    /**
     *
     */
    interface ListenerEmitter {

        /**
         * @param output
         */
        void emit(Object output);
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
        void accept(
            Connection connection, Map<String, ?> inputParameters, String workflowExecutionId,
            ListenerEmitter listenerEmitter);
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
     */
    interface StaticWebhookRequestContext {

        /**
         *
         * @return
         */
        Map<String, ?> inputParameters();

        /**
         *
         * @return
         */
        HttpHeaders headers();

        /**
         *
         * @return
         */
        HttpParameters parameters();

        /**
         *
         * @return
         */
        WebhookBody body();

        /**
         *
         * @return
         */
        WebhookMethod method();

        /**
         *
         * @return
         */
        TriggerContext triggerContext();
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
    interface TriggerContext extends Context {
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
    interface WebhookBody {

        /**
         *
         * @return
         */
        Object content();

        /**
         *
         * @return
         */
        ContentType contentType();

        /**
         *
         * @return
         */
        String mimeType();

        /**
         *
         */
        enum ContentType {
            BINARY,
            FORM_DATA,
            FORM_URL_ENCODED,
            JSON,
            RAW,
            XML
        }
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
    interface WebhookValidateContext {

        /**
         *
         * @return
         */
        Map<String, ?> inputParameters();

        /**
         *
         * @return
         */
        HttpHeaders headers();

        /**
         *
         * @return
         */
        HttpParameters parameters();

        /**
         *
         * @return
         */
        WebhookBody body();

        /**
         *
         * @return
         */
        WebhookMethod method();

        /**
         *
         * @return
         */
        TriggerContext triggerContext();
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
