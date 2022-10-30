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

package com.bytechef.rest.error;

import com.bytechef.rest.error.vm.FieldErrorVM;
import com.bytechef.rest.utils.HeaderUtils;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.zalando.problem.DefaultProblem;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemBuilder;
import org.zalando.problem.Status;
import org.zalando.problem.StatusType;
import org.zalando.problem.spring.webflux.advice.ProblemHandling;
import org.zalando.problem.violations.ConstraintViolationProblem;
import reactor.core.publisher.Mono;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 *
 * @author Ivica Cardic
 */
@RestControllerAdvice
public class ExceptionTranslator implements ProblemHandling /*, SecurityAdviceTrait*/ {

    private static final String FIELD_ERRORS_KEY = "fieldErrors";
    private static final String MESSAGE_KEY = "message";
    private static final String PATH_KEY = "path";
    private static final String VIOLATIONS_KEY = "violations";

    @Value("${spring.application.name}")
    private String applicationName;

    private final Environment env;

    public ExceptionTranslator(Environment env) {
        this.env = env;
    }

    /**
     * Post-process the Problem payload to add the message key for the front-end if needed.
     */
    @Override
    public Mono<ResponseEntity<Problem>> process(
            @Nullable ResponseEntity<Problem> responseEntity, ServerWebExchange serverWebExchange) {
        if (responseEntity == null) {
            return null;
        }

        Problem problem = responseEntity.getBody();

        if (!(problem instanceof ConstraintViolationProblem || problem instanceof DefaultProblem)) {
            return Mono.just(responseEntity);
        }

        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();

        String requestUri = serverHttpRequest.getURI().toString();

        ProblemBuilder builder = Problem.builder()
                .withType(
                        Problem.DEFAULT_TYPE.equals(problem.getType())
                                ? ErrorConstants.DEFAULT_TYPE
                                : problem.getType())
                .withStatus(problem.getStatus())
                .withTitle(problem.getTitle())
                .with(PATH_KEY, requestUri);

        if (problem instanceof ConstraintViolationProblem) {
            builder.with(VIOLATIONS_KEY, ((ConstraintViolationProblem) problem).getViolations())
                    .with(MESSAGE_KEY, ErrorConstants.ERR_VALIDATION);
        } else {
            builder.withCause(((DefaultProblem) problem).getCause())
                    .withDetail(problem.getDetail())
                    .withInstance(problem.getInstance());

            Map<String, Object> parameters = problem.getParameters();
            StatusType statusType = problem.getStatus();

            parameters.forEach(builder::with);

            if (!parameters.containsKey(MESSAGE_KEY) && statusType != null) {
                builder.with(MESSAGE_KEY, "error.http." + statusType.getStatusCode());
            }
        }

        return Mono.just(
                new ResponseEntity<>(builder.build(), responseEntity.getHeaders(), responseEntity.getStatusCode()));
    }

    @ExceptionHandler
    public Mono<ResponseEntity<Problem>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException methodArgumentNotValidException,
            @Nonnull ServerWebExchange serverWebExchange) {
        BindingResult result = methodArgumentNotValidException.getBindingResult();

        List<FieldErrorVM> fieldErrors = result.getFieldErrors().stream()
                .map(f -> new FieldErrorVM(
                        f.getObjectName().replaceFirst("DTO$", ""),
                        f.getField(),
                        StringUtils.isNotBlank(f.getDefaultMessage()) ? f.getDefaultMessage() : f.getCode()))
                .collect(Collectors.toList());

        Problem problem = Problem.builder()
                .withType(ErrorConstants.CONSTRAINT_VIOLATION_TYPE)
                .withTitle("Method argument not valid")
                .withStatus(defaultConstraintViolationStatus())
                .with(MESSAGE_KEY, ErrorConstants.ERR_VALIDATION)
                .with(FIELD_ERRORS_KEY, fieldErrors)
                .build();

        return create(methodArgumentNotValidException, problem, serverWebExchange);
    }

    @ExceptionHandler
    public Mono<ResponseEntity<Problem>> handleBadRequestAlertException(
            BadRequestAlertException badRequestAlertException, ServerWebExchange serverWebExchange) {
        return create(
                badRequestAlertException,
                serverWebExchange,
                HeaderUtils.createFailureAlert(
                        applicationName,
                        true,
                        badRequestAlertException.getEntityName(),
                        badRequestAlertException.getErrorKey(),
                        badRequestAlertException.getMessage()));
    }

    @ExceptionHandler
    public Mono<ResponseEntity<Problem>> handleConcurrencyFailure(
            ConcurrencyFailureException concurrencyFailureException, ServerWebExchange serverWebExchange) {
        Problem problem = Problem.builder()
                .withStatus(Status.CONFLICT)
                .with(MESSAGE_KEY, ErrorConstants.ERR_CONCURRENCY_FAILURE)
                .build();

        return create(concurrencyFailureException, problem, serverWebExchange);
    }

    @Override
    public ProblemBuilder prepare(final Throwable throwable, final StatusType status, final URI type) {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());

        if (activeProfiles.contains("prod")) {
            if (throwable instanceof HttpMessageConversionException) {
                return Problem.builder()
                        .withType(type)
                        .withTitle(status.getReasonPhrase())
                        .withStatus(status)
                        .withDetail("Unable to convert http message")
                        .withCause(Optional.ofNullable(throwable.getCause())
                                .filter(cause -> isCausalChainsEnabled())
                                .map(this::toProblem)
                                .orElse(null));
            }
            if (throwable instanceof DataAccessException) {
                return Problem.builder()
                        .withType(type)
                        .withTitle(status.getReasonPhrase())
                        .withStatus(status)
                        .withDetail("Failure during data access")
                        .withCause(Optional.ofNullable(throwable.getCause())
                                .filter(cause -> isCausalChainsEnabled())
                                .map(this::toProblem)
                                .orElse(null));
            }
            if (containsPackageName(throwable.getMessage())) {
                return Problem.builder()
                        .withType(type)
                        .withTitle(status.getReasonPhrase())
                        .withStatus(status)
                        .withDetail("Unexpected runtime exception")
                        .withCause(Optional.ofNullable(throwable.getCause())
                                .filter(cause -> isCausalChainsEnabled())
                                .map(this::toProblem)
                                .orElse(null));
            }
        }

        return Problem.builder()
                .withType(type)
                .withTitle(status.getReasonPhrase())
                .withStatus(status)
                .withDetail(throwable.getMessage())
                .withCause(Optional.ofNullable(throwable.getCause())
                        .filter(cause -> isCausalChainsEnabled())
                        .map(this::toProblem)
                        .orElse(null));
    }

    private boolean containsPackageName(String message) {
        // This list is for sure not complete
        return StringUtils.containsAny(message, "org.", "java.", "net.", "javax.", "com.", "io.", "de.");
    }
}
