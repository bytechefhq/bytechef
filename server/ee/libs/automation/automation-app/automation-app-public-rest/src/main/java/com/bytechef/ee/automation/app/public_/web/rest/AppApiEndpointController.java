/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.handler.web.rest.AbstractApiPlatformEndpointController;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppApiExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.AbstractAppApiController.ErrorBody;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.exception.AbstractException;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Calls API Platform endpoints of the collections bound to the calling app:
 * {@code /api/app/v1/api/v{collectionVersion}/{contextPath}/{endpointPath}}. Only bound collections that resolve as
 * available are searched, in the app's own environment; the {@code X-ENVIRONMENT} header is ignored. The status,
 * headers and body the endpoint's workflow chooses pass through unchanged. An unmatched path answers 404
 * {@code APP_API_ENDPOINT_NOT_FOUND}, without revealing whether an unbound collection would have matched; a path under
 * an unavailable bound collection, or a matched endpoint whose workflow or deployment is disabled, answers 409
 * {@code APP_API_COLLECTION_UNAVAILABLE}. A workflow that fails or times out answers a marked 500
 * {@code APP_EXECUTION_FAILED} or 504 {@code APP_EXECUTION_TIMEOUT}. A workflow cannot set the
 * {@code X-ByteChef-App-Error} header, so a client can trust it marks ByteChef's own errors.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppApiEndpointController")
@RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1/api/**")
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class AppApiEndpointController extends AbstractApiPlatformEndpointController {

    private static final Logger log = LoggerFactory.getLogger(AppApiEndpointController.class);

    private final AppApiExecutionFacade appApiExecutionFacade;
    private final String basePath;

    @SuppressFBWarnings("EI")
    public AppApiEndpointController(
        ApiCollectionEndpointService apiCollectionEndpointService, AppApiExecutionFacade appApiExecutionFacade,
        ApplicationProperties applicationProperties, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, TempFileStorage tempFileStorage,
        WebhookWorkflowExecutor webhookWorkflowExecutor, WorkflowService workflowService,
        @Value("${openapi.openAPIDefinition.base-path.app:/api/app}") String appBasePath) {

        super(
            apiCollectionEndpointService, applicationProperties, projectDeploymentService,
            projectDeploymentWorkflowService, projectWorkflowService, tempFileStorage, webhookWorkflowExecutor,
            workflowService);

        this.appApiExecutionFacade = appApiExecutionFacade;
        this.basePath = appBasePath + "/v1/api";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> handleAccessDeniedException(AccessDeniedException accessDeniedException) {
        return AbstractAppApiController.toResponse(
            HttpStatus.FORBIDDEN, "ACCESS_DENIED", accessDeniedException.getMessage());
    }

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ErrorBody> handleConfigurationException(ConfigurationException configurationException) {
        return AbstractAppApiController.translate(configurationException);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleException(Exception exception) {
        Throwable cause = unwrap(exception);

        if (cause instanceof AccessDeniedException accessDeniedException) {
            return handleAccessDeniedException(accessDeniedException);
        }

        if (cause instanceof AbstractException abstractException) {
            return AbstractAppApiController.translate(abstractException);
        }

        if (cause instanceof ProviderException providerException) {
            return AbstractAppApiController.toResponse(
                HttpStatus.UNPROCESSABLE_ENTITY, "PROVIDER_ERROR", providerException.getMessage());
        }

        if (cause instanceof TimeoutException) {
            log.warn("App API endpoint workflow timed out", exception);

            return toResponse(
                HttpStatus.GATEWAY_TIMEOUT, AppErrorType.APP_EXECUTION_TIMEOUT,
                "The endpoint's workflow did not complete in time");
        }

        log.error("App API endpoint workflow failed", exception);

        return toResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, AppErrorType.APP_EXECUTION_FAILED, "The endpoint's workflow failed");
    }

    @Override
    protected ResponseEntity<?> doHandle(
        HttpMethod httpMethod, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        return super.doHandle(
            httpMethod, httpServletRequest, new AppErrorHeaderStrippingResponse(httpServletResponse));
    }

    @Override
    protected ResponseEntity<?> executeEndpoint(
        ApiCollection apiCollection, ApiCollectionEndpoint apiCollectionEndpoint,
        Supplier<ResponseEntity<?>> execution) {

        ResponseEntity<?> responseEntity = appApiExecutionFacade.recordApiCall(
            apiCollection.getName(), apiCollectionEndpoint.getName(), execution,
            executionResponseEntity -> executionResponseEntity.getStatusCode()
                .value());

        return withoutAppErrorHeader(responseEntity);
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }

    @Override
    protected Environment getEnvironment(HttpServletRequest httpServletRequest) {
        AppPrincipal appPrincipal = AppPrincipal.getCurrent();

        return appPrincipal.environment();
    }

    @Override
    protected List<ApiCollection> getSearchableApiCollections(Environment environment) {
        return appApiExecutionFacade.getCallableApiCollections();
    }

    @Override
    protected ResponseEntity<?> notFound(String path) {
        for (ApiCollection apiCollection : appApiExecutionFacade.getUnavailableApiCollections()) {
            String collectionPathPrefix = getCollectionPathPrefix(apiCollection);

            if (path.equals(collectionPathPrefix) || path.startsWith(collectionPathPrefix + "/")) {
                return unavailable("API collection '%s' is unavailable".formatted(apiCollection.getName()));
            }
        }

        return toResponse(
            HttpStatus.NOT_FOUND, AppErrorType.APP_API_ENDPOINT_NOT_FOUND,
            "No endpoint of an API collection bound to the app matches " + path);
    }

    @Override
    protected ResponseEntity<?> unavailable(String message) {
        return toResponse(HttpStatus.CONFLICT, AppErrorType.APP_API_COLLECTION_UNAVAILABLE, message);
    }

    private static ResponseEntity<ErrorBody> toResponse(HttpStatus status, AppErrorType appErrorType, String message) {
        return AbstractAppApiController.toResponse(status, AppErrorType.nameOf(appErrorType.getErrorKey()), message);
    }

    private static Throwable unwrap(Throwable throwable) {
        Set<Throwable> visitedThrowables = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;

        while (isWrapper(current) && current.getCause() != null && visitedThrowables.add(current)) {
            current = current.getCause();
        }

        return current;
    }

    private static boolean isWrapper(Throwable throwable) {
        return throwable.getClass() == RuntimeException.class || throwable instanceof CompletionException ||
            throwable instanceof ExecutionException;
    }

    private static ResponseEntity<?> withoutAppErrorHeader(ResponseEntity<?> responseEntity) {
        HttpHeaders headers = responseEntity.getHeaders();

        if (!headers.containsHeader(AbstractAppApiController.APP_ERROR_HEADER)) {
            return responseEntity;
        }

        HttpHeaders strippedHeaders = new HttpHeaders();

        strippedHeaders.putAll(headers);

        strippedHeaders.remove(AbstractAppApiController.APP_ERROR_HEADER);

        return new ResponseEntity<>(responseEntity.getBody(), strippedHeaders, responseEntity.getStatusCode());
    }

    /**
     * Drops the app error marker from the headers a workflow writes onto the response.
     */
    private static final class AppErrorHeaderStrippingResponse extends HttpServletResponseWrapper {

        private AppErrorHeaderStrippingResponse(HttpServletResponse httpServletResponse) {
            super(httpServletResponse);
        }

        @Override
        public void addHeader(String name, String value) {
            if (!isAppErrorHeader(name)) {
                super.addHeader(name, value);
            }
        }

        @Override
        public void setHeader(String name, String value) {
            if (!isAppErrorHeader(name)) {
                super.setHeader(name, value);
            }
        }

        private static boolean isAppErrorHeader(String name) {
            return AbstractAppApiController.APP_ERROR_HEADER.equalsIgnoreCase(name);
        }
    }
}
