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

import com.bytechef.component.exception.ProviderException;
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
    @FunctionalInterface
    interface WebhookDisableConsumer {

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
    interface WebhookEnableFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param webhookUrl
         * @param workflowExecutionId
         * @return
         */
        WebhookEnableOutput apply(
            Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
            TriggerContext context);

    }

    /**
     *
     * @param parameters
     * @param webhookExpirationDate
     */
    @SuppressFBWarnings("EI")
    record WebhookEnableOutput(Map<String, ?> parameters, LocalDateTime webhookExpirationDate) {
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
    Optional<WebhookDisableConsumer> getWebhookDisable();

    /**
     *
     * @return
     */
    Optional<WebhookEnableFunction> getWebhookEnable();

    /**
     *
     * @return
     */
    Optional<DynamicWebhookRefreshFunction> getDynamicWebhookRefresh();

    /**
     *
     * @return
     */
    Optional<WebhookRequestFunction> getDWebhookRequest();

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
    Optional<TriggerOutputFunction> getOutput();

    /**
     *
     * @return
     */
    Optional<OutputResponse> getOutputResponse();

    /**
     *
     * @return
     */
    Optional<PollFunction> getPoll();

    /**
     *
     * @return
     */
    Optional<TriggerDefinition.ProcessErrorResponseFunction> getProcessErrorResponse();

    /**
     *
     * @return
     */
    Optional<List<? extends Property>> getProperties();

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
     * @return
     */
    Optional<Boolean> getWebhookRawBody();

    /**
     *
     * @return
     */
    Optional<WebhookValidateFunction> getWebhookValidate();

    /**
     *
     * @return
     */
    Optional<WebhookValidateOnEnableFunction> getWebhookValidateOnEnable();

    /**
     *
     * @return
     */
    Optional<TriggerWorkflowNodeDescriptionFunction> getWorkflowNodeDescription();

    /**
     * @return
     */
    Optional<Boolean> getWorkflowSyncExecution();

    /**
     * @return
     */
    Optional<Boolean> getWorkflowSyncValidation();

    /**
     *
     * @return
     */
    Optional<Boolean> getWorkflowSyncOnEnableValidation();

    /**
     *
     * @return
     */
    boolean isDynamicOutput();

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
    interface DynamicWebhookRefreshFunction {

        WebhookEnableOutput apply(Parameters outputParameters, TriggerContext context);
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
    interface ListenerTriggerOutputFunction extends TriggerOutputFunction {

        /**
         * @param inputParameters
         * @param connectionParameters
         * @param workflowExecutionId
         */
        OutputResponse accept(
            Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
            TriggerContext context) throws Exception;
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
            Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
            TriggerContext context)
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
    interface PollTriggerOutputFunction extends TriggerOutputFunction {

        /**
         * @param inputParameters
         * @param closureParameters
         * @param context
         * @return
         */
        OutputResponse apply(Parameters inputParameters, Parameters closureParameters, TriggerContext context)
            throws Exception;

    }

    /**
     *
     */
    @FunctionalInterface
    interface ProcessErrorResponseFunction {

        /**
         *
         * @param statusCode
         * @param body
         * @param context
         * @return
         */
        ProviderException apply(int statusCode, Object body, Context context) throws Exception;
    }

    /**
     *
     */
    interface TriggerOutputFunction {
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
         * @return
         */
        String getRawContent();

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
    interface WebhookRequestFunction {

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
            HttpParameters parameters, WebhookBody body, WebhookMethod method, WebhookEnableOutput output,
            TriggerContext context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface WebhookTriggerOutputFunction extends TriggerOutputFunction {

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
        OutputResponse apply(
            Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
            HttpParameters parameters, WebhookBody body, WebhookMethod method, WebhookEnableOutput output,
            TriggerContext context) throws Exception;
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
         * @return the http status, 200 if validation is ok, 400, 401 or any other required status if validation fails
         */
        WebhookValidateResponse apply(
            Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
            WebhookMethod method, TriggerContext context);

    }

    /**
     *
     */
    @FunctionalInterface
    interface WebhookValidateOnEnableFunction {
        /**
         *
         * @param inputParameters
         * @param headers
         * @param parameters
         * @param body
         * @param method
         * @param context
         * @return the http status, 200 if validation is ok, 400, 401 or any other required status if validation fails
         */
        WebhookValidateResponse apply(
            Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
            WebhookMethod method, TriggerContext context);

    }

    @SuppressFBWarnings("EI")
    record WebhookValidateResponse(Object body, Map<String, List<String>> headers, int status) {

        public WebhookValidateResponse(int status) {
            this(null, null, status);
        }

        public WebhookValidateResponse(Object body, int status) {
            this(body, null, status);
        }

        public static WebhookValidateResponse badRequest() {
            return new WebhookValidateResponse(HttpStatus.BAD_REQUEST.getValue());
        }

        public static WebhookValidateResponse ok() {
            return new WebhookValidateResponse(HttpStatus.OK.getValue());
        }

        public static WebhookValidateResponse ok(Object body) {
            return new WebhookValidateResponse(body, null, HttpStatus.OK.getValue());
        }

        public static WebhookValidateResponse unauthorized() {
            return new WebhookValidateResponse(HttpStatus.UNAUTHORIZED.getValue());
        }
    }
}
