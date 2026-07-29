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

package com.bytechef.component.definition;

import com.bytechef.component.exception.ProviderException;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.definition.BaseOutputFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Defines the contract for a component trigger, the event source that initiates a workflow. A trigger declares its
 * metadata (name, title, description, help, properties, output) together with the functions that implement its
 * behavior, which vary by {@link TriggerType} (polling, static or dynamic webhook, listener, and so on).
 *
 * @author Ivica Cardic
 */
public interface TriggerDefinition {

    /**
     * Enumerates the kinds of triggers supported by the platform, determining how a trigger receives events and which
     * of its functions are invoked at runtime.
     */
    enum TriggerType {
        CALLABLE,
        DYNAMIC_WEBHOOK,
        HYBRID,
        LISTENER,
        POLLING,
        STATIC_WEBHOOK,
        WEBSOCKET
    }

    /**
     * Enumerates the HTTP methods a webhook request may use when it reaches a webhook-based trigger.
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
     * Returns the description of this trigger, shown to users to explain what the trigger does.
     *
     * @return an {@link Optional} containing the description, or an empty {@link Optional} if none is set
     */
    Optional<String> getDescription();

    /**
     * Returns the function that refreshes a dynamic webhook subscription before it expires, when defined for a
     * dynamic-webhook trigger.
     *
     * @return an {@link Optional} containing the refresh function, or an empty {@link Optional} if none is set
     */
    Optional<DynamicWebhookRefreshFunction> getDynamicWebhookRefresh();

    /**
     * TODO
     *
     * @return
     */
    Optional<DeduplicateFunction> getDeduplicate();

    /**
     * Returns the contextual help associated with this trigger.
     *
     * @return an {@link Optional} containing the trigger help, or an empty {@link Optional} if none is set
     */
    Optional<Help> getHelp();

    /**
     * Returns the consumer invoked to tear down a webhook subscription when the trigger is disabled, when defined for a
     * webhook trigger.
     *
     * @return an {@link Optional} containing the webhook-disable consumer, or an empty {@link Optional} if none is set
     */
    Optional<WebhookDisableConsumer> getWebhookDisable();

    /**
     * Returns the function invoked to register a webhook subscription when the trigger is enabled, when defined for a
     * webhook trigger.
     *
     * @return an {@link Optional} containing the webhook-enable function, or an empty {@link Optional} if none is set
     */
    Optional<WebhookEnableFunction> getWebhookEnable();

    /**
     * Returns the function that processes an incoming webhook request and converts it into the trigger's output, when
     * defined for a webhook trigger.
     *
     * @return an {@link Optional} containing the webhook-request function, or an empty {@link Optional} if none is set
     */
    Optional<WebhookRequestFunction> getWebhookRequest();

    /**
     * Returns the technical name that uniquely identifies this trigger within its component.
     *
     * @return the trigger name
     */
    String getName();

    /**
     * Returns the consumer invoked to stop an event listener when the trigger is disabled, when defined for a listener
     * trigger.
     *
     * @return an {@link Optional} containing the listener-disable consumer, or an empty {@link Optional} if none is set
     */
    Optional<ListenerDisableConsumer> getListenerDisable();

    /**
     * Returns the consumer invoked to start an event listener when the trigger is enabled, when defined for a listener
     * trigger.
     *
     * @return an {@link Optional} containing the listener-enable consumer, or an empty {@link Optional} if none is set
     */
    Optional<ListenerEnableConsumer> getListenerEnable();

    /**
     * Returns the definition describing the shape of the data this trigger produces.
     *
     * @return an {@link Optional} containing the output definition, or an empty {@link Optional} if none is set
     */
    Optional<OutputDefinition> getOutputDefinition();

    /**
     * Returns the function that polls the provider for new records, when defined for a polling trigger.
     *
     * @return an {@link Optional} containing the poll function, or an empty {@link Optional} if none is set
     */
    Optional<PollFunction> getPoll();

    /**
     * Returns the function that translates a provider error response into a {@link ProviderException}, when defined.
     *
     * @return an {@link Optional} containing the error-response function, or an empty {@link Optional} if none is set
     */
    Optional<TriggerDefinition.ProcessErrorResponseFunction> getProcessErrorResponse();

    /**
     * Returns the input properties the user configures for this trigger.
     *
     * @return an {@link Optional} containing the list of properties, or an empty {@link Optional} if none are defined
     */
    Optional<List<? extends Property>> getProperties();

    /**
     * Returns the human-readable title of this trigger, displayed in the workflow editor.
     *
     * @return an {@link Optional} containing the title, or an empty {@link Optional} if none is set
     */
    Optional<String> getTitle();

    /**
     * Returns the {@link TriggerType} that determines how this trigger receives events.
     *
     * @return the trigger type
     */
    TriggerType getType();

    /**
     * Indicates whether the raw, unparsed request body should be preserved for webhook processing.
     *
     * @return an {@link Optional} containing {@code true} to keep the raw body, or an empty {@link Optional} if not
     *         specified
     */
    Optional<Boolean> getWebhookRawBody();

    /**
     * Returns the function that validates each incoming webhook request, when defined for a webhook trigger.
     *
     * @return an {@link Optional} containing the webhook-validate function, or an empty {@link Optional} if none is set
     */
    Optional<WebhookValidateFunction> getWebhookValidate();

    /**
     * Returns the function that validates the webhook request received during the enable handshake, when defined for a
     * webhook trigger.
     *
     * @return an {@link Optional} containing the on-enable webhook-validate function, or an empty {@link Optional} if
     *         none is set
     */
    Optional<WebhookValidateOnEnableFunction> getWebhookValidateOnEnable();

    /**
     * Returns the function that computes a dynamic description for this trigger's workflow node based on its configured
     * inputs, when defined.
     *
     * @return an {@link Optional} containing the node-description function, or an empty {@link Optional} if none is set
     */
    Optional<WorkflowNodeDescriptionFunction> getWorkflowNodeDescription();

    /**
     * Indicates whether the workflow started by this trigger should be executed synchronously.
     *
     * @return an {@link Optional} containing {@code true} for synchronous execution, or an empty {@link Optional} if
     *         not specified
     */
    Optional<Boolean> getWorkflowSyncExecution();

    /**
     * Computes a stable deduplication key for a polled record so that records already delivered are not emitted again.
     */
    interface DeduplicateFunction {

        /**
         * Computes the deduplication key for the given record.
         *
         * @param record the polled record to derive a key from
         * @return a stable identifier used to detect and skip duplicate records
         */
        String apply(Object record);
    }

    /**
     * Refreshes an existing dynamic webhook subscription before it expires, returning updated enable output.
     */
    @FunctionalInterface
    interface DynamicWebhookRefreshFunction {

        /**
         * Refreshes the webhook subscription and returns the updated enable output.
         *
         * @param connectionParameters          the connection parameters
         * @param webhookEnableOutputParameters the parameters produced by the original webhook enable call
         * @param context                       the trigger execution context
         * @return the refreshed webhook enable output, including the new expiration date
         */
        WebhookEnableOutput
            apply(Parameters connectionParameters, Parameters webhookEnableOutputParameters, TriggerContext context);
    }

    /**
     * Provides read access to the HTTP headers of an incoming webhook request, where each header name may map to
     * multiple values.
     */
    interface HttpHeaders {

        /**
         * Returns all values associated with the given header name.
         *
         * @param name the header name
         * @return the list of values for the header, or an empty list if the header is absent
         */
        List<String> allValues(String name);

        /**
         * Returns the first value associated with the given header name, if present.
         *
         * @param name the header name
         * @return an {@link Optional} containing the first header value, or an empty {@link Optional} if absent
         */
        Optional<String> firstValue(String name);

        /**
         * Returns the first value associated with the given header name parsed as a long, if present.
         *
         * @param name the header name
         * @return an {@link OptionalLong} containing the parsed value, or an empty {@link OptionalLong} if absent
         */
        OptionalLong firstValueAsLong(String name);

        /**
         * Returns all headers as a map from header name to its list of values.
         *
         * @return a map view of all headers
         */
        Map<String, List<String>> toMap();
    }

    /**
     * Provides read access to the query parameters of an incoming webhook request, where each parameter name may map to
     * multiple values.
     */
    interface HttpParameters {

        /**
         * Returns all values associated with the given parameter name.
         *
         * @param name the parameter name
         * @return the list of values for the parameter, or an empty list if the parameter is absent
         */
        List<String> allValues(String name);

        /**
         * Returns the first value associated with the given parameter name, if present.
         *
         * @param name the parameter name
         * @return an {@link Optional} containing the first parameter value, or an empty {@link Optional} if absent
         */
        Optional<String> firstValue(String name);

        /**
         * Returns the first value associated with the given parameter name parsed as a long, if present.
         *
         * @param name the parameter name
         * @return an {@link OptionalLong} containing the parsed value, or an empty {@link OptionalLong} if absent
         */
        OptionalLong firstValueAsLong(String name);

        /**
         * Returns all query parameters as a map from parameter name to its list of values.
         *
         * @return a map view of all query parameters
         */
        Map<String, List<String>> toMap();
    }

    /**
     * Stops the event listener associated with a listener trigger when the trigger is disabled.
     */
    @FunctionalInterface
    interface ListenerDisableConsumer {

        /**
         * Tears down the listener for the given workflow execution.
         *
         * @param inputParameters      the trigger input parameters
         * @param connectionParameters the connection parameters
         * @param workflowExecutionId  the identifier of the workflow execution the listener was bound to
         * @param context              the trigger execution context
         * @throws Exception if the listener cannot be disabled
         */
        void accept(
            Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
            TriggerContext context) throws Exception;
    }

    /**
     * Allows an active event listener to push received events into the workflow as trigger output.
     */
    interface ListenerEmitter {

        /**
         * Emits a received event as the trigger's output, starting a workflow execution.
         *
         * @param output the event payload to emit
         */
        void emit(Object output);
    }

    /**
     * Starts the event listener associated with a listener trigger when the trigger is enabled, wiring received events
     * to the supplied {@link ListenerEmitter}.
     */
    @FunctionalInterface
    interface ListenerEnableConsumer {

        /**
         * Sets up the listener for the given workflow execution.
         *
         * @param inputParameters      the trigger input parameters
         * @param connectionParameters the connection parameters
         * @param workflowExecutionId  the identifier of the workflow execution the listener is bound to
         * @param listenerEmitter      the emitter through which received events are pushed into the workflow
         * @param context              the trigger execution context
         * @throws Exception if the listener cannot be enabled
         */
        void accept(
            Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
            ListenerEmitter listenerEmitter, TriggerContext context) throws Exception;
    }

    /**
     * Dynamically resolves the selectable options for a trigger property, optionally filtered by user-entered search
     * text and dependent on the values of other properties.
     *
     * @param <T> the type of the option values produced
     */
    @FunctionalInterface
    interface OptionsFunction<T> extends OptionsDataSource.BaseOptionsFunction {

        /**
         * Computes the list of options available for a property.
         *
         * @param inputParameters      the trigger input parameters
         * @param connectionParameters the connection parameters
         * @param lookupDependsOnPaths the current values of the properties this lookup depends on, keyed by path
         * @param searchText           the text the user has entered to filter options, or {@code null} if none
         * @param context              the trigger execution context
         * @return the list of resolved options
         * @throws Exception if the options cannot be resolved
         */
        List<? extends Option<T>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
            String searchText, TriggerContext context) throws Exception;
    }

    /**
     * Polls the provider for new records on behalf of a polling trigger, using closure parameters to carry state (such
     * as a cursor) from one poll to the next.
     */
    @FunctionalInterface
    interface PollFunction {

        /**
         * Performs a single poll and returns the records found along with the state to carry into the next poll.
         *
         * @param inputParameters      the trigger input parameters
         * @param connectionParameters the connection parameters
         * @param closureParameters    the state carried over from the previous poll (for example, a cursor)
         * @param context              the trigger execution context
         * @return the poll output containing new records and updated closure parameters
         * @throws Exception if the poll fails
         */
        PollOutput apply(
            Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
            TriggerContext context)
            throws Exception;
    }

    /**
     * Dynamically resolves a set of trigger properties at runtime, potentially depending on the values of other
     * properties.
     */
    @FunctionalInterface
    interface PropertiesFunction extends PropertiesDataSource.BasePropertiesFunction {

        /**
         * Computes the dynamic properties to present to the user.
         *
         * @param inputParameters      the trigger input parameters
         * @param connectionParameters the connection parameters
         * @param lookupDependsOnPaths the current values of the properties this lookup depends on, keyed by path
         * @param context              the trigger execution context
         * @return the list of dynamically resolved properties
         * @throws Exception if the properties cannot be resolved
         */
        List<? extends Property.ValueProperty<?>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
            TriggerContext context) throws Exception;
    }

    /**
     * Translates an error response returned by the provider into a {@link ProviderException}, allowing the trigger to
     * classify and enrich failures.
     */
    @FunctionalInterface
    interface ProcessErrorResponseFunction {

        /**
         * Builds a {@link ProviderException} describing the provider's error response.
         *
         * @param statusCode the HTTP status code of the error response
         * @param body       the response body, if any
         * @param headers    the response headers, keyed by name
         * @param context    the trigger execution context
         * @return the provider exception representing the error
         * @throws Exception if the error response cannot be processed
         */
        ProviderException apply(int statusCode, Object body, Map<String, List<String>> headers, TriggerContext context)
            throws Exception;
    }

    /**
     * Computes a human-readable description for this trigger's workflow node, derived from its configured inputs.
     */
    @FunctionalInterface
    interface WorkflowNodeDescriptionFunction {

        /**
         * Computes the node description from the configured inputs.
         *
         * @param inputParameters the trigger input parameters
         * @param context         the trigger execution context
         * @return the description to display for the workflow node
         * @throws Exception if the description cannot be computed
         */
        String apply(Parameters inputParameters, TriggerContext context) throws Exception;
    }

    /**
     * Computes the output schema and sample for a trigger at runtime, when the shape of the output depends on the
     * trigger's configuration.
     */
    interface OutputFunction extends BaseOutputFunction {

        /**
         * Computes the trigger's output response.
         *
         * @param inputParameters      the trigger input parameters
         * @param connectionParameters the connection parameters
         * @param context              the trigger execution context
         * @return the resolved output response describing the trigger's output
         * @throws Exception if the output cannot be computed
         */
        OutputResponse apply(Parameters inputParameters, Parameters connectionParameters, TriggerContext context)
            throws Exception;
    }

    /**
     * Represents the body of an incoming webhook request, exposing its parsed content in various forms along with
     * metadata describing how the body was encoded.
     */
    interface WebhookBody {

        /**
         * Returns the parsed content of the request body.
         *
         * @return the body content
         */
        Object getContent();

        /**
         * Returns the request body content converted to the given type.
         *
         * @param valueType the type the content should be converted to
         * @param <T>       the requested content type
         * @return the converted content
         */
        <T> T getContent(Class<T> valueType);

        /**
         * Returns the request body content converted using the supplied {@link TypeReference} to preserve generic type
         * information.
         *
         * @param valueTypeRef the type reference describing the target type
         * @param <T>          the requested content type
         * @return the converted content
         */
        <T> T getContent(TypeReference<T> valueTypeRef);

        /**
         * Returns the content type describing how the body was encoded.
         *
         * @return the body content type
         */
        ContentType getContentType();

        /**
         * Returns the MIME type declared for the request body.
         *
         * @return the MIME type string
         */
        String getMimeType();

        /**
         * Returns the raw, unparsed request body as a string.
         *
         * @return the raw body content
         */
        String getRawContent();

        /**
         * Enumerates the supported encodings of a webhook request body.
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
     * Removes a webhook subscription from the provider when a webhook trigger is disabled.
     */
    @FunctionalInterface
    interface WebhookDisableConsumer {

        /**
         * Tears down the webhook subscription for the given workflow execution.
         *
         * @param inputParameters               the trigger input parameters
         * @param connectionParameters          the connection parameters
         * @param webhookEnableOutputParameters the parameters produced when the webhook was enabled
         * @param workflowExecutionId           the identifier of the workflow execution the webhook was bound to
         * @param context                       the trigger execution context
         */
        void accept(
            Parameters inputParameters, Parameters connectionParameters, Parameters webhookEnableOutputParameters,
            String workflowExecutionId, TriggerContext context);

    }

    /**
     * Registers a webhook subscription with the provider when a webhook trigger is enabled, directing events to the
     * supplied callback URL.
     */
    @FunctionalInterface
    interface WebhookEnableFunction {

        /**
         * Registers the webhook subscription and returns the resulting enable output.
         *
         * @param inputParameters      the trigger input parameters
         * @param connectionParameters the connection parameters
         * @param webhookUrl           the callback URL the provider should send events to
         * @param workflowExecutionId  the identifier of the workflow execution the webhook is bound to
         * @param context              the trigger execution context
         * @return the webhook enable output, including any parameters and expiration date to retain
         */
        WebhookEnableOutput apply(
            Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
            TriggerContext context);

    }

    /**
     * Processes an incoming webhook request for a webhook trigger and produces the output that starts the workflow.
     */
    @FunctionalInterface
    interface WebhookRequestFunction {

        /**
         * Handles the incoming webhook request and returns the trigger output.
         *
         * @param inputParameters               the trigger input parameters
         * @param connectionParameters          the connection parameters
         * @param headers                       the request headers
         * @param parameters                    the request query parameters
         * @param body                          the request body
         * @param method                        the HTTP method of the request
         * @param webhookEnableOutputParameters the parameters produced when the webhook was enabled
         * @param context                       the trigger execution context
         * @return the output derived from the request, used as the trigger's output
         * @throws Exception if the request cannot be processed
         */
        Object apply(
            Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
            HttpParameters parameters, WebhookBody body, WebhookMethod method, Parameters webhookEnableOutputParameters,
            TriggerContext context) throws Exception;
    }

    /**
     * Validates each incoming webhook request before it is processed, allowing a webhook trigger to reject unauthorized
     * or malformed requests.
     */
    @FunctionalInterface
    interface WebhookValidateFunction {
        /**
         * Validates the incoming webhook request.
         *
         * @param inputParameters the trigger input parameters
         * @param headers         the request headers
         * @param parameters      the request query parameters
         * @param body            the request body
         * @param method          the HTTP method of the request
         * @param context         the trigger execution context
         * @return the http status, 200 if validation is ok, 400, 401 or any other required status if validation fails
         */
        WebhookValidateResponse apply(
            Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
            WebhookMethod method, TriggerContext context);

    }

    /**
     * Validates the webhook request received during the enable handshake, allowing a webhook trigger to confirm the
     * subscription before it is activated.
     */
    @FunctionalInterface
    interface WebhookValidateOnEnableFunction {
        /**
         * Validates the webhook request received while enabling the subscription.
         *
         * @param inputParameters the trigger input parameters
         * @param headers         the request headers
         * @param parameters      the request query parameters
         * @param body            the request body
         * @param method          the HTTP method of the request
         * @param context         the trigger execution context
         * @return the http status, 200 if validation is ok, 400, 401 or any other required status if validation fails
         */
        WebhookValidateResponse apply(
            Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
            WebhookMethod method, TriggerContext context);

    }

    /**
     * Carries the result of a single poll: the records found, the state to carry into the next poll, and whether the
     * next poll should run immediately.
     *
     * @param records           the records discovered by this poll
     * @param closureParameters the state (for example, a cursor) to pass to the next poll
     * @param pollImmediately   {@code true} to trigger the next poll right away rather than waiting for the schedule
     */
    @SuppressFBWarnings("EI")
    record PollOutput(List<?> records, Map<String, ?> closureParameters, boolean pollImmediately) {
    }

    /**
     * Carries the result of enabling a webhook subscription: provider-specific parameters to retain and, optionally,
     * the moment the subscription expires and must be refreshed.
     *
     * @param parameters            the parameters to retain for later refresh and disable calls
     * @param webhookExpirationDate the instant at which the subscription expires, or {@code null} if it does not expire
     */
    @SuppressFBWarnings("EI")
    record WebhookEnableOutput(Map<String, ?> parameters, Instant webhookExpirationDate) {
    }

    /**
     * Represents the response returned by a webhook validation, pairing an HTTP status with an optional body and
     * headers to send back to the caller.
     *
     * @param body    the response body to return to the caller, or {@code null} for none
     * @param headers the response headers to return to the caller, or {@code null} for none
     * @param status  the HTTP status code communicating the validation result
     */
    @SuppressFBWarnings("EI")
    record WebhookValidateResponse(Object body, Map<String, List<String>> headers, int status) {

        /**
         * Creates a validation response carrying only an HTTP status, with no body or headers.
         *
         * @param status the HTTP status code
         */
        public WebhookValidateResponse(int status) {
            this(null, null, status);
        }

        /**
         * Creates a validation response carrying a body and an HTTP status, with no headers.
         *
         * @param body   the response body
         * @param status the HTTP status code
         */
        public WebhookValidateResponse(Object body, int status) {
            this(body, null, status);
        }

        /**
         * Creates a validation response indicating the request is invalid (HTTP 400 Bad Request).
         *
         * @return a bad-request validation response
         */
        public static WebhookValidateResponse badRequest() {
            return new WebhookValidateResponse(HttpStatus.BAD_REQUEST.getValue());
        }

        /**
         * Creates a validation response indicating the request is valid (HTTP 200 OK).
         *
         * @return a successful validation response
         */
        public static WebhookValidateResponse ok() {
            return new WebhookValidateResponse(HttpStatus.OK.getValue());
        }

        /**
         * Creates a successful validation response (HTTP 200 OK) carrying the given body.
         *
         * @param body the response body to return to the caller
         * @return a successful validation response with the given body
         */
        public static WebhookValidateResponse ok(Object body) {
            return new WebhookValidateResponse(body, null, HttpStatus.OK.getValue());
        }

        /**
         * Creates a validation response indicating the request is not authorized (HTTP 401 Unauthorized).
         *
         * @return an unauthorized validation response
         */
        public static WebhookValidateResponse unauthorized() {
            return new WebhookValidateResponse(HttpStatus.UNAUTHORIZED.getValue());
        }
    }
}
