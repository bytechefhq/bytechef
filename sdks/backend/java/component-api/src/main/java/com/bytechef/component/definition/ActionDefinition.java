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

import static com.bytechef.component.definition.ActionDefinition.WebhookResponse.Type.BINARY;
import static com.bytechef.component.definition.ActionDefinition.WebhookResponse.Type.JSON;
import static com.bytechef.component.definition.ActionDefinition.WebhookResponse.Type.NO_DATA;
import static com.bytechef.component.definition.ActionDefinition.WebhookResponse.Type.RAW;
import static com.bytechef.component.definition.ActionDefinition.WebhookResponse.Type.REDIRECT;

import com.bytechef.component.exception.ProviderException;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinition {

    /**
     * TODO
     *
     * @return
     */
    Optional<Boolean> getBatch();

    /**
     * Retrieves an optional {@link BeforeResumeFunction}, which represents a custom operation to be executed before
     * resuming an action in a workflow or process. This function can be utilized for validations, data transformations,
     * or other pre-processing tasks related to the continuation of the action.
     *
     * @return an {@code Optional} containing the {@link BeforeResumeFunction} if defined, or an empty {@code Optional}
     *         if not present
     */
    Optional<BeforeResumeFunction> getBeforeResume();

    /**
     * Retrieves an optional {@link BeforeSuspendConsumer} that represents a custom operation to be executed before
     * suspending an action in a workflow or process. This consumer can be utilized for tasks such as preparing
     * suspension parameters or executing custom pre-suspension logic specific to the action.
     *
     * @return an {@code Optional} containing the {@link BeforeSuspendConsumer} if defined, or an empty {@code Optional}
     *         if not present
     */
    Optional<BeforeSuspendConsumer> getBeforeSuspend();

    /**
     * Retrieves an optional {@link BeforeTimeoutResumeFunction}, which represents a custom operation to be executed
     * before a timeout occurs and allows for resumption of processing with specific parameters. This function serves as
     * a mechanism to handle timeout scenarios by potentially modifying processing parameters or performing other
     * context-specific actions.
     *
     * @return an {@code Optional} containing the {@link BeforeTimeoutResumeFunction} if defined, or an empty
     *         {@code Optional} if not present
     */
    Optional<BeforeTimeoutResumeFunction> getBeforeTimeoutResume();

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
     *
     * @return
     */
    Optional<Help> getHelp();

    /**
     *
     * @return
     */
    Optional<Map<String, Object>> getMetadata();

    /**
     *
     * @return
     */
    String getName();

    /**
     *
     * @return
     */
    Optional<OutputDefinition> getOutputDefinition();

    /**
     * The code that should be executed when an action runs as a task inside the workflow engine.
     *
     * @return an optional execute function implementation
     */
    Optional<? extends BasePerformFunction> getPerform();

    /**
     *
     * @return
     */
    Optional<ProcessErrorResponseFunction> getProcessErrorResponse();

    /**
     *
     * @return
     */
    Optional<List<? extends Property>> getProperties();

    Optional<ResumePerformFunction> getResumePerform();

    Optional<SuspendPerformFunction> getSuspendPerform();

    /**
     *
     * @return
     */
    Optional<String> getTitle();

    /**
     *
     * @return
     */
    Optional<WorkflowNodeDescriptionFunction> getWorkflowNodeDescription();

    /**
     * Represents a base interface for defining output functions within the system. This interface serves as a
     * foundational contract for more specialized output function definitions, enabling consistency and extendability in
     * handling output-related operations. <br>
     * Implementations of this interface are expected to be stateless and thread-safe, ensuring that they can be reused
     * across various contexts without introducing side effects or concurrency issues. <br>
     * It extends the {@code com.bytechef.definition.BaseOutputFunction}, inheriting its core functionalities while
     * allowing for additional behavior to be implemented by child interfaces or classes.
     */
    interface BaseOutputFunction extends com.bytechef.definition.BaseOutputFunction {

    }

    /**
     * Represents the base interface for defining custom action execution logic. Implementations of this interface
     * typically serve as a foundational contract for creating functional interfaces that require the execution of
     * specific actions within a defined context, using parameters and configuration. <br>
     * This interface is designed to be extended by more specific functional interfaces to enable the dynamic execution
     * of business processes, workflows, or other operations.
     */
    interface BasePerformFunction {

    }

    /**
     * Functional interface representing a consumer to be invoked prior to suspending an action execution. This
     * interface is designed to provide custom actions that can be executed before a suspension event occurs. It
     * provides parameters related to the current context, suspension details, and options to perform further actions.
     */
    @FunctionalInterface
    interface BeforeSuspendConsumer {

        /**
         * Applies a specific action during the execution flow, handling suspension details and resumption parameters.
         * This method is invoked with the current action context, allowing for custom logic to be executed before the
         * action is suspended for resumption.
         *
         * @param resumeUrl          The URL to be used for resuming the action after suspension.
         * @param expiresAt          The expiration time indicating how long the suspension is valid.
         * @param continueParameters The parameters passed for continuation, containing key-value data for resumption.
         * @param context            The current action context containing execution data.
         * @throws Exception If an error occurs during execution.
         */
        void apply(String resumeUrl, Instant expiresAt, Parameters continueParameters, ActionContext context)
            throws Exception;
    }

    /**
     * Represents a functional interface for defining a custom operation that is executed before an action resumes in a
     * specific workflow or process. The operation takes input data, input parameters, continuation parameters, and an
     * action context as arguments, and produces an output encapsulated in a continue {@link Map} instance. This
     * functional interface can be leveraged to integrate pre-resume processing logic, such as validations,
     * transformations, or enrichments, as part of the action workflow.
     */
    @FunctionalInterface
    interface BeforeResumeFunction {

        /**
         * Applies a custom operation before an action resumes in a workflow or process. This method processes the
         * provided input data, input parameters, continue parameters, and action context to produce an optional
         * continue {@link Map} object. It can be used for operations like validation, data enrichment, or preparation
         * before resuming the workflow.
         *
         * @param data               the input data passed to the method for processing
         * @param inputParameters    the parameters related to the initial invocation of the action
         * @param continueParameters the parameters related to the continuation of the action
         * @param context            the contextual information about the current workflow or process
         * @return an optional continue {@link Map} instance containing processed continuation parameters
         * @throws Exception if any error occurs during the processing of the operation
         */
        Optional<Map<String, ?>> apply(
            Object data, Parameters inputParameters, Parameters continueParameters, ActionContext context)
            throws Exception;
    }

    /**
     * Functional interface representing a function that is executed before a timeout occurs and allows for resumption
     * of processing with specific parameters.
     */
    @FunctionalInterface
    interface BeforeTimeoutResumeFunction {

        /**
         * Applies the function using the provided input parameters, continuation parameters, and action context. The
         * function may modify the processing based on the inputs and may return specific parameters for further
         * processing.
         *
         * @param inputParameters    the parameters provided as input to the function
         * @param continueParameters the parameters used for continuation of processing
         * @param context            the action context which provides additional contextual information for the
         *                           function execution
         * @return an {@link Optional} containing continue {@link Map} instance to be used for further actions, or an
         *         empty {@link Optional} if no result is to be returned
         * @throws Exception if an error occurs during function execution
         */
        Optional<Map<String, ?>> apply(Parameters inputParameters, Parameters continueParameters, ActionContext context)
            throws Exception;
    }

    /**
     * Functional interface for handling Server-Sent Events (SSE) within an action or application. This interface
     * provides a mechanism to emit data to clients in real-time by invoking the {@code handle} method with an
     * appropriate {@link SseEmitter} implementation.
     */
    @FunctionalInterface
    interface SseEmitterHandler {

        /**
         * Handles the emission of server-sent events (SSE) by processing and managing the event stream through the
         * provided emitter. This method is responsible for controlling the lifecycle of the event emission, including
         * sending data, handling completion, errors, and timeout scenarios.
         *
         * @param sseEmitter the {@code Emitter} instance used to manage and emit server-sent events to connected
         *                   clients. This emitter provides methods for sending data, completing the event stream,
         *                   handling errors, and registering timeout listeners.
         */
        void handle(SseEmitter sseEmitter);

        /**
         * Represents an abstraction for handling server-sent events (SSE) by allowing data emission to connected
         * clients and managing event lifecycle events such as completion, errors, and timeouts.
         */
        interface SseEmitter {

            /**
             * Registers a listener to be invoked when a timeout event occurs.
             *
             * @param timeoutListener a {@code Runnable} to be executed upon a timeout event. This listener should
             *                        contain the logic to handle or respond to the timeout condition.
             */
            void addTimeoutListener(Runnable timeoutListener);

            /**
             * Marks the completion of the event emission process. Once this method is called, the emitter signals that
             * no further events will be sent to the connected clients, and the lifecycle of the connection or event
             * stream is concluded. Any subsequent attempts to emit events after invoking this method will typically
             * result in a runtime error or be ignored depending on the implementation.
             */
            void complete();

            /**
             * Marks the event emission process as completed with an error. This method signals the occurrence of an
             * error during the event lifecycle, preventing further events from being sent to the connected clients.
             * Once invoked, the emitter transitions into an error state and terminates the connection or event stream
             * with the provided exception.
             *
             * @param throwable the {@code Throwable} instance representing the error or exception that caused the
             *                  termination of the event emission process. It provides context about the failure for
             *                  logging or client-side handling.
             */
            void error(Throwable throwable);

            /**
             * Sends the specified data to the connected clients or destination. This method handles the emission of
             * data for server-sent events (SSE) or similar mechanisms as supported by the implementation. The exact
             * behavior of this method, such as handling of errors, buffering, or serialization, depends on the specific
             * implementation of the emitter interface.
             *
             * @param data the object containing the data to be sent. This could include any type of object that is
             *             serializable or transmittable according to the underlying emitter's implementation. It is the
             *             caller's responsibility to ensure compatibility of the provided data with the expected format
             *             or protocol.
             */
            void send(Object data);
        }
    }

    /**
     *
     */
    @FunctionalInterface
    interface OptionsFunction<T> extends OptionsDataSource.BaseOptionsFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param lookupDependsOnPaths
         * @param searchText
         * @param context
         * @return
         * @throws Exception
         */
        List<? extends Option<T>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
            String searchText, ActionContext context) throws Exception;
    }

    /**
     * Represents a specialized output function interface that processes input and connection parameters along with
     * additional context to produce a standardized output response. <br>
     * This interface extends the {@code BaseOutputFunction} to define a specific method for generating an
     * {@code OutputResponse}. <br>
     * Implementations of this interface are responsible for handling the logic required to process various inputs and
     * contexts, returning a meaningful and structured output.
     */
    interface OutputFunction extends BaseOutputFunction {

        /**
         * Processes the given input parameters, connection parameters, and context to generate an output response.
         *
         * @param inputParameters      the parameters specific to the input data being processed
         * @param connectionParameters the parameters required for connection or external resource access
         * @param context              additional context about the execution environment or process
         * @return an {@code OutputResponse} representing the result of the operation, including schema, sample output,
         *         and optional placeholder
         * @throws Exception if an error occurs during the processing
         */
        OutputResponse apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
            throws Exception;

    }

    /**
     * Functional interface defining the contract for executing a specific action with given input parameters,
     * connection parameters, and action context, and returning a result upon completion. <br>
     * The primary purpose of this interface is to provide a flexible mechanism for implementing custom business logic
     * that can be executed dynamically within workflows or processes.
     */
    @FunctionalInterface
    interface PerformFunction extends BasePerformFunction {

        /**
         * Applies the specified action using the given input parameters, connection parameters, and action context.
         *
         * @param inputParameters      the input parameters for the action
         * @param connectionParameters the parameters related to the connection or configuration
         * @param context              the context in which the action is executed
         * @return the result of the action execution as an Object
         * @throws Exception if an error occurs during the execution of the action
         */
        Object apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
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
         * @param actionContext
         * @return
         */
        ProviderException apply(int statusCode, Object body, ActionContext actionContext) throws Exception;

    }

    /**
     *
     */
    @FunctionalInterface
    interface PropertiesFunction extends PropertiesDataSource.BasePropertiesFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param lookupDependsOnPaths
         * @param context
         * @return
         * @throws Exception
         */
        List<? extends Property.ValueProperty<?>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
            ActionContext context) throws Exception;
    }

    /**
     * Represents a functional interface intended for performing a continuation or resume operation with a single
     * connection within a workflow or procedural execution context. This interface extends {@code PerformFunction},
     * adding support for actions that require input parameters, connection-related parameters, continuation parameters,
     * and an action context. <br>
     * Implementations of this interface are designed to handle operations that resume from a specific state or context
     * during workflow execution.
     */
    @FunctionalInterface
    interface ResumePerformFunction {

        /**
         * Executes an action using the provided parameters and context. This method is designed for continuation or
         * resuming action in a workflow or procedural execution.
         *
         * @param inputParameters      the parameters specific to the operation or input data
         * @param connectionParameters the parameters related to the connection or external resource required for
         *                             execution
         * @param continueParameters   the parameters associated with the continuation process or resume state
         * @param context              the action context providing access to auxiliary data, event functions, or state
         *                             management facilities
         * @return the result of the action execution, which can be any object as per the operation's requirements
         * @throws Exception if an error occurs during execution or processing of the action
         */
        Object apply(
            Parameters inputParameters, Parameters connectionParameters, Parameters continueParameters,
            ActionContext context)

            throws Exception;
    }

    /**
     * Functional interface that extends {@link PerformFunction} to support streaming operations that return
     * asynchronous event streams during action execution. <br/>
     * This interface is specifically designed for actions that need to stream multiple events or data chunks over time
     * rather than returning a single result. It returns an {@link SseEmitterHandler} that bridges the action's
     * streaming data to SSE events throughout the duration of the action execution. <br/>
     * Implementations of this interface should return an {@link SseEmitterHandler} that bridges the action's streaming
     * data to SSE events, allowing consumers to receive streamed data as it becomes available. This is particularly
     * useful for long-running operations, real-time data processing, or scenarios where results need to be delivered
     * incrementally rather than all at once. <br/>
     * The returned {@link SseEmitterHandler} will continue to stream events for the entire duration of the action
     * execution, completing when the action is finished or terminating with an error if the action fails.
     *
     * @see PerformFunction
     * @see SseEmitterHandler
     * @see ActionContext
     * @see Parameters
     */
    @FunctionalInterface
    interface StreamPerformFunction extends PerformFunction {

        /**
         * Applies the streaming perform function to execute an action and returns an {@link SseEmitterHandler} used to
         * stream events. <br/>
         * This method executes the action with the provided input and connection parameters within the given context,
         * returning an {@link SseEmitterHandler} that bridges the action's streaming data to SSE events sent to
         * clients.
         *
         * @param inputParameters      the input parameters for the action execution, containing the data required to
         *                             perform the action
         * @param connectionParameters the connection parameters containing authentication and configuration details
         *                             required to connect to external services
         * @param context              the action execution context providing access to runtime environment,
         *                             configuration, and utility services
         * @return the {@link SseEmitterHandler} that will stream events for the duration of this action
         * @throws Exception if an error occurs during action execution or setup
         */
        SseEmitterHandler apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
            throws Exception;
    }

    /**
     * A perform function variant that returns {@link SseEmitterHandler} used to stream events to clients during the
     * action execution. The workflow engine will wait until the returned emitter signals completion before proceeding
     * to the next task.
     */
    @FunctionalInterface
    interface SseStreamResponsePerformFunction extends PerformFunction {

        /**
         * Execute the action and return an {@link SseEmitterHandler} used to stream events.
         *
         * @param inputParameters      the input parameters for the action
         * @param connectionParameters the parameters related to the connection
         * @param context              the context in which the action is executed
         * @return the {@link SseEmitterHandler} that will stream events for the duration of this action
         * @throws Exception if an error occurs during action execution or setup
         */
        SseEmitterHandler apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
            throws Exception;
    }

    /**
     * Represents a specialized functional interface within the ActionDefinition framework, used for defining
     * suspendable action implementations. This interface extends {@link BasePerformFunction} and provides a mechanism
     * for suspending actions during their execution.
     */
    @FunctionalInterface
    interface SuspendPerformFunction {

        /**
         * Applies the specified input parameters, connection parameters, and action context to perform an operation
         * that results in a suspendable state.
         *
         * @param inputParameters      the parameters provided as input for the operation
         * @param connectionParameters the parameters required to establish a connection or interaction
         * @param context              the context in which the action is executed, providing runtime information
         * @return a {@link Suspend} object representing the suspendable state, including continuation parameters and
         *         expiration details
         * @throws Exception if an error occurs during the execution of the operation
         */
        Suspend apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
            throws Exception;
    }

    /**
     * A functional interface that extends PerformFunction. Represents a specific type of function designed to handle
     * webhook responses within the context of an action. This interface defines a single abstract method that takes
     * input parameters, connection parameters, and an action context to produce a WebhookResponse.
     */
    @FunctionalInterface
    interface WebhookResponsePerformFunction extends PerformFunction {

        /**
         * Applies the given input parameters, connection parameters, and action context to produce a WebhookResponse.
         *
         * @param inputParameters      the input parameters required for processing
         * @param connectionParameters the connection-specific parameters necessary for execution
         * @param context              the context of the action that provides additional environmental details
         * @return a WebhookResponse object containing the result of the operation
         * @throws Exception if an error occurs during processing
         */
        WebhookResponse apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
            throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface WorkflowNodeDescriptionFunction {

        /**
         * @param inputParameters
         * @param context
         * @return
         */
        String apply(Parameters inputParameters, ActionContext context) throws Exception;
    }

    /**
     * Represents a suspend state as part of an action definition. It indicates a temporary suspension with accompanying
     * parameters and expiration details.
     *
     * @param continueParameters a map containing parameters required to continue the suspended action
     * @param expiresAt          the timestamp indicating when the suspension expires
     */
    @SuppressFBWarnings("EI")
    record Suspend(Map<String, ?> continueParameters, Instant expiresAt) {
    }

    /**
     * Represents an HTTP response returned by a webhook action.
     * <p>
     * This class provides factory methods to construct various types of HTTP responses, including JSON, raw text,
     * binary data, redirects, and responses with no body content. Each response can be customized with HTTP headers and
     * status codes.
     * </p>
     *
     * <h2>Usage Examples:</h2>
     *
     * <pre>{@code
     * // Simple JSON response
     * WebhookResponse response = WebhookResponse.json(Map.of("status", "success"));
     *
     * // JSON with custom status code
     * WebhookResponse created = WebhookResponse.json(data, HttpStatus.CREATED);
     *
     * // JSON with headers and status
     * WebhookResponse customJson = WebhookResponse.json(
     *     data,
     *     Map.of("X-Custom-Header", "value"),
     *     HttpStatus.OK);
     *
     * // Raw text response
     * WebhookResponse text = WebhookResponse.raw("Plain text content");
     *
     * // Binary file response
     * WebhookResponse file = WebhookResponse.binary(fileEntry);
     *
     * // Redirect response
     * WebhookResponse redirect = WebhookResponse.redirect("https://example.com");
     *
     * // No content response with headers
     * WebhookResponse noContent = WebhookResponse.noData(
     *     Map.of("X-Request-Id", "123"),
     *     HttpStatus.NO_CONTENT);
     * }</pre>
     *
     * @author Ivica Cardic
     */
    class WebhookResponse {

        /**
         * Defines the type of content in the webhook response body.
         */
        public enum Type {
            /** JSON-formatted content that will be serialized to JSON */
            JSON,
            /** Raw text content (e.g., plain text, HTML, XML) */
            RAW,
            /** Binary file content */
            BINARY,
            /** HTTP redirect to another URL */
            REDIRECT,
            /** No response body content */
            NO_DATA
        }

        private Object body;
        private Map<String, String> headers = Map.of();
        private int statusCode;
        private Type type;

        private WebhookResponse() {
        }

        private WebhookResponse(Type type, Object body, Map<String, String> headers, int statusCode) {
            this.type = type;
            this.body = body;
            this.headers = Collections.unmodifiableMap(headers);
            this.statusCode = statusCode;
        }

        /**
         * Creates a JSON response with HTTP 200 OK status.
         *
         * @param body the response body that will be serialized to JSON (typically a Map, List, or POJO)
         * @return a new WebhookResponse instance with JSON content type and 200 status
         */
        public static WebhookResponse json(Object body) {
            return new WebhookResponse(JSON, body, Map.of(), HttpStatus.OK.getValue());
        }

        /**
         * Creates a JSON response with custom headers and HTTP 200 OK status.
         *
         * @param body    the response body that will be serialized to JSON
         * @param headers custom HTTP headers to include in the response
         * @return a new WebhookResponse instance with JSON content type, custom headers, and 200 status
         */
        public static WebhookResponse json(Object body, Map<String, String> headers) {
            return new WebhookResponse(JSON, body, headers, HttpStatus.OK.getValue());
        }

        /**
         * Creates a JSON response with a custom status code.
         *
         * @param body       the response body that will be serialized to JSON
         * @param statusCode the HTTP status code (e.g., 201, 400, 500)
         * @return a new WebhookResponse instance with JSON content type and custom status code
         */
        public static WebhookResponse json(Object body, int statusCode) {
            return new WebhookResponse(JSON, body, Map.of(), statusCode);
        }

        /**
         * Creates a JSON response with a custom HTTP status.
         *
         * @param body   the response body that will be serialized to JSON
         * @param status the HTTP status enum value
         * @return a new WebhookResponse instance with JSON content type and custom status
         */
        public static WebhookResponse json(Object body, HttpStatus status) {
            return new WebhookResponse(JSON, body, Map.of(), status.getValue());
        }

        /**
         * Creates a JSON response with custom headers and status code.
         *
         * @param body       the response body that will be serialized to JSON
         * @param headers    custom HTTP headers to include in the response
         * @param statusCode the HTTP status code
         * @return a new WebhookResponse instance with JSON content type, custom headers, and status code
         */
        public static WebhookResponse json(Object body, Map<String, String> headers, int statusCode) {
            return new WebhookResponse(JSON, body, headers, statusCode);
        }

        /**
         * Creates a JSON response with custom headers and HTTP status.
         *
         * @param body    the response body that will be serialized to JSON
         * @param headers custom HTTP headers to include in the response
         * @param status  the HTTP status enum value
         * @return a new WebhookResponse instance with JSON content type, custom headers, and status
         */
        public static WebhookResponse json(Object body, Map<String, String> headers, HttpStatus status) {
            return new WebhookResponse(JSON, body, headers, status.getValue());
        }

        /**
         * Creates a raw text response with HTTP 200 OK status.
         * <p>
         * Use this method for plain text, HTML, XML, or any other string-based content.
         * </p>
         *
         * @param body the raw text content
         * @return a new WebhookResponse instance with raw content type and 200 status
         */
        public static WebhookResponse raw(String body) {
            return new WebhookResponse(RAW, body, Map.of(), HttpStatus.OK.getValue());
        }

        /**
         * Creates a raw text response with custom headers and HTTP 200 OK status.
         *
         * @param body    the raw text content
         * @param headers custom HTTP headers to include in the response
         * @return a new WebhookResponse instance with raw content type, custom headers, and 200 status
         */
        public static WebhookResponse raw(String body, Map<String, String> headers) {
            return new WebhookResponse(RAW, body, headers, HttpStatus.OK.getValue());
        }

        /**
         * Creates a raw text response with a custom status code.
         *
         * @param body   the raw text content
         * @param status the HTTP status code
         * @return a new WebhookResponse instance with raw content type and custom status code
         */
        public static WebhookResponse raw(String body, int status) {
            return new WebhookResponse(RAW, body, Map.of(), status);
        }

        /**
         * Creates a raw text response with a custom HTTP status.
         *
         * @param body   the raw text content
         * @param status the HTTP status enum value
         * @return a new WebhookResponse instance with raw content type and custom status
         */
        public static WebhookResponse raw(String body, HttpStatus status) {
            return new WebhookResponse(RAW, body, Map.of(), status.getValue());
        }

        /**
         * Creates a raw text response with custom headers and status code.
         *
         * @param body       the raw text content
         * @param headers    custom HTTP headers to include in the response
         * @param statusCode the HTTP status code
         * @return a new WebhookResponse instance with raw content type, custom headers, and status code
         */
        public static WebhookResponse raw(String body, Map<String, String> headers, int statusCode) {
            return new WebhookResponse(RAW, body, headers, statusCode);
        }

        /**
         * Creates a raw text response with custom headers and HTTP status.
         *
         * @param body    the raw text content
         * @param headers custom HTTP headers to include in the response
         * @param status  the HTTP status enum value
         * @return a new WebhookResponse instance with raw content type, custom headers, and status
         */
        public static WebhookResponse raw(String body, Map<String, String> headers, HttpStatus status) {
            return new WebhookResponse(RAW, body, headers, status.getValue());
        }

        /**
         * Creates a binary file response with HTTP 200 OK status.
         *
         * @param body the binary file content
         * @return a new WebhookResponse instance with binary content type and 200 status
         */
        public static WebhookResponse binary(FileEntry body) {
            return new WebhookResponse(BINARY, body, Map.of(), HttpStatus.OK.getValue());
        }

        /**
         * Creates a binary file response with custom headers and HTTP 200 OK status.
         *
         * @param body    the binary file content
         * @param headers custom HTTP headers to include in the response (e.g., Content-Type, Content-Disposition)
         * @return a new WebhookResponse instance with binary content type, custom headers, and 200 status
         */
        public static WebhookResponse binary(FileEntry body, Map<String, String> headers) {
            return new WebhookResponse(BINARY, body, headers, HttpStatus.OK.getValue());
        }

        /**
         * Creates a binary file response with custom headers and status code.
         *
         * @param body       the binary file content
         * @param headers    custom HTTP headers to include in the response
         * @param statusCode the HTTP status code
         * @return a new WebhookResponse instance with binary content type, custom headers, and status code
         */
        public static WebhookResponse binary(FileEntry body, Map<String, String> headers, int statusCode) {
            return new WebhookResponse(BINARY, body, headers, statusCode);
        }

        /**
         * Creates a binary file response with custom headers and HTTP status.
         *
         * @param body    the binary file content
         * @param headers custom HTTP headers to include in the response
         * @param status  the HTTP status enum value
         * @return a new WebhookResponse instance with binary content type, custom headers, and status
         */
        public static WebhookResponse binary(FileEntry body, Map<String, String> headers, HttpStatus status) {
            return new WebhookResponse(BINARY, body, headers, status.getValue());
        }

        /**
         * Creates an HTTP redirect response (302 Found).
         *
         * @param url the URL to redirect to
         * @return a new WebhookResponse instance with redirect type and 302 status
         */
        public static WebhookResponse redirect(String url) {
            return new WebhookResponse(REDIRECT, url, Map.of(), HttpStatus.FOUND.getValue());
        }

        /**
         * Creates a response with no body content and custom headers with HTTP 200 OK status.
         * <p>
         * Useful for responses that only need to return headers without a body.
         * </p>
         *
         * @param headers custom HTTP headers to include in the response
         * @return a new WebhookResponse instance with no body, custom headers, and 200 status
         */
        public static WebhookResponse noData(Map<String, String> headers) {
            return new WebhookResponse(NO_DATA, null, headers, 200);
        }

        /**
         * Creates a response with no body content and custom headers with a custom status code.
         * <p>
         * Useful for responses like 204 No Content or 304 Not Modified.
         * </p>
         *
         * @param headers    custom HTTP headers to include in the response
         * @param statusCode the HTTP status code (e.g., 204, 304)
         * @return a new WebhookResponse instance with no body, custom headers, and custom status code
         */
        public static WebhookResponse noData(Map<String, String> headers, int statusCode) {
            return new WebhookResponse(NO_DATA, null, headers, statusCode);
        }

        /**
         * Creates a response with no body content and custom headers with a custom HTTP status.
         *
         * @param headers custom HTTP headers to include in the response
         * @param status  the HTTP status enum value
         * @return a new WebhookResponse instance with no body, custom headers, and custom status
         */
        public static WebhookResponse noData(Map<String, String> headers, HttpStatus status) {
            return new WebhookResponse(NO_DATA, null, headers, status.getValue());
        }

        /**
         * Returns the response body.
         * <p>
         * The body type depends on the response type:
         * </p>
         * <ul>
         * <li>JSON: any object that can be serialized to JSON (Map, List, POJO, etc.)</li>
         * <li>RAW: String containing text content</li>
         * <li>BINARY: FileEntry containing binary file data</li>
         * <li>REDIRECT: String containing the redirect URL</li>
         * <li>NO_DATA: null</li>
         * </ul>
         *
         * @return the response body, or null for NO_DATA responses
         */
        public Object getBody() {
            return body;
        }

        /**
         * Returns an unmodifiable map of HTTP headers.
         *
         * @return immutable map of HTTP header names to values
         */
        public Map<String, String> getHeaders() {
            return Collections.unmodifiableMap(headers);
        }

        /**
         * Returns the HTTP status code.
         *
         * @return the HTTP status code (e.g., 200, 201, 404, 500)
         */
        public int getStatusCode() {
            return statusCode;
        }

        /**
         * Returns the response type.
         *
         * @return the type of content in the response body
         * @see Type
         */
        public Type getType() {
            return type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }

            var that = (WebhookResponse) obj;

            return Objects.equals(this.body, that.body) && Objects.equals(this.headers, that.headers) &&
                this.statusCode == that.statusCode && Objects.equals(this.type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, body, headers, statusCode);
        }

        @Override
        public String toString() {
            return "WebhookResponse[" +
                "type=" + type + ", " +
                "body=" + body + ", " +
                "headers=" + headers + ", " +
                "statusCode=" + statusCode + ']';
        }
    }
}
