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

    /**
     *
     * @return
     */
    Optional<String> getComponentDescription();

    /**
     *
     * @return
     */
    String getComponentName();

    /**
     *
     * @return
     */
    Optional<String> getComponentTitle();

    /**
     *
     * @return
     */
    int getComponentVersion();

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
    Optional<TriggerNodeDescriptionFunction> getNodeDescriptionFunction();

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
    Optional<Output> getOutput();

    /**
     *
     * @return
     */
    Optional<TriggerOutputFunction> getOutputFunction();

    /**
     *
     * @return
     */
    Optional<PollFunction> getPoll();

    /**
     *
     * @return
     */
    Optional<List<? extends Property>> getProperties();

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
     * @return
     */
    boolean isDefaultOutputFunction();

    /**
     *
     */
    interface DeduplicateFunction {

        /**
         *
         * @param record
         * @return
         */
        String apply(Object record);
    }

    /**
     *
     */
    @FunctionalInterface
    interface DynamicWebhookDisableConsumer {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param outputParameters
         * @param workflowExecutionId
         */
        void accept(
            Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
            String workflowExecutionId, TriggerContext context);

    }

    /**
     *
     */
    @FunctionalInterface
    interface DynamicWebhookEnableFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param webhookUrl
         * @param workflowExecutionId
         * @return
         */
        DynamicWebhookEnableOutput apply(
            Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
            String workflowExecutionId, TriggerContext context);

    }

    /**
     *
     * @param parameters
     * @param webhookExpirationDate
     */
    @SuppressFBWarnings("EI")
    record DynamicWebhookEnableOutput(Map<String, ?> parameters, LocalDateTime webhookExpirationDate) {
    }

    /**
     *
     */
    @FunctionalInterface
    interface DynamicWebhookRefreshFunction {

        DynamicWebhookEnableOutput apply(Parameters outputParameters, TriggerContext context);
    }

    /**
     *
     */
    @FunctionalInterface
    interface DynamicWebhookRequestFunction {

        /**
         *
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
        Object apply(
            Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
            HttpParameters parameters, WebhookBody body, WebhookMethod method, DynamicWebhookEnableOutput output,
            TriggerContext context) throws Exception;
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
        Map<String, List<String>> toMap();
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
        Map<String, List<String>> toMap();
    }

    /**
     *
     */
    @FunctionalInterface
    interface ListenerDisableConsumer {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param workflowExecutionId
         */
        void accept(
            Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
            TriggerContext context) throws Exception;
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
         *
         * @param inputParameters
         * @param connectionParameters
         * @param workflowExecutionId
         * @param listenerEmitter
         */
        void accept(
            Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
            ListenerEmitter listenerEmitter, TriggerContext context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface PollFunction {

        /**
         *
         * @param inputParameters
         * @param closureParameters
         * @param context
         * @return
         */
        PollOutput apply(
            Parameters inputParameters, Parameters closureParameters, TriggerContext context)
            throws Exception;

    }

    /**
     *
     * @param records
     * @param closureParameters
     * @param pollImmediately
     */
    @SuppressFBWarnings("EI")
    record PollOutput(List<?> records, Map<String, ?> closureParameters, boolean pollImmediately) {
        public List<?> getRecords() {
            return records;
        }
    }

    /**
     *
     */
    @FunctionalInterface
    interface StaticWebhookRequestFunction {

        /**
         *
         * @param inputParameters
         * @param headers
         * @param parameters
         * @param body
         * @param method
         * @param context
         * @return
         */
        Object apply(
            Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
            WebhookMethod method, TriggerContext context) throws Exception;

    }

    /**
     *
     */
    interface WebhookBody {

        /**
         *
         * @return
         */
        Object getContent();

        <T> T getContent(Class<T> valueType);

        <T> T getContent(Context.TypeReference<T> valueTypeRef);

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
    @FunctionalInterface
    interface WebhookValidateFunction {

        /**
         *
         * @param inputParameters
         * @param headers
         * @param parameters
         * @param body
         * @param method
         * @param context
         * @return
         */
        boolean apply(
            Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
            WebhookMethod method, TriggerContext context);
    }
}
