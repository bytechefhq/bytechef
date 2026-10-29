/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.exception.TitleGenerationFailedException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLType;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Resolves AI Hub exceptions that the CE {@code GlobalDataFetcherExceptionResolver} cannot see, because the EE
 * exception types are not on its classpath. Without this bean they fall through to an opaque {@code INTERNAL_ERROR}
 * logged at ERROR with a full stack.
 *
 * <p>
 * Mapping: {@link NotFoundException} to {@code NOT_FOUND}, {@link ForbiddenException} to {@code FORBIDDEN},
 * {@link ConflictException} to {@code CONFLICT}, and {@link TitleGenerationFailedException} to {@code INTERNAL_ERROR}
 * with a typed {@code errorCode}. The generic {@link AccessDeniedException} ({@code FORBIDDEN}) and
 * {@link IllegalArgumentException} ({@code BAD_REQUEST}) are mapped only for a hub field, a field whose name carries
 * {@code aiHub} / {@code AiHub} or whose parent type is an {@code AiHub*} type, so every other GraphQL operation keeps
 * the global resolver's behaviour. The not-found and forbidden messages are fixed strings, so the error shape never
 * reveals whether a chat exists or which resource was probed.
 * </p>
 *
 * <p>
 * The title-generation message is deliberately generic: the underlying provider error can carry account and billing
 * detail which must not reach the browser.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnAiHub
class AiHubGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    static final String ACCESS_DENIED_ERROR_CODE = "ACCESS_DENIED";
    static final String BAD_REQUEST_ERROR_CODE = "BAD_REQUEST";
    static final String CONFLICT_ERROR_CODE = "CONFLICT";
    static final String NOT_FOUND_ERROR_CODE = "NOT_FOUND";
    static final String TITLE_GENERATION_FAILED_ERROR_CODE = "TITLE_GENERATION_FAILED";

    private static final Logger log = LoggerFactory.getLogger(AiHubGraphQlExceptionResolver.class);

    @Override
    protected @Nullable GraphQLError resolveToSingleError(
        Throwable throwable, DataFetchingEnvironment dataFetchingEnvironment) {

        if (throwable instanceof NotFoundException) {
            return error(dataFetchingEnvironment, ErrorType.NOT_FOUND, "Not found", NOT_FOUND_ERROR_CODE);
        }

        if (throwable instanceof ForbiddenException) {
            return error(dataFetchingEnvironment, ErrorType.FORBIDDEN, "Access denied", ACCESS_DENIED_ERROR_CODE);
        }

        if (throwable instanceof ConflictException) {
            return error(
                dataFetchingEnvironment, AiHubErrorType.CONFLICT, throwable.getMessage(), CONFLICT_ERROR_CODE);
        }

        if (throwable instanceof TitleGenerationFailedException) {
            log.warn("Chat title generation failed: {}", throwable.getMessage());

            return error(
                dataFetchingEnvironment, ErrorType.INTERNAL_ERROR, "Chat title generation is temporarily unavailable.",
                TITLE_GENERATION_FAILED_ERROR_CODE);
        }

        if (!isHubField(dataFetchingEnvironment)) {
            return null;
        }

        if (throwable instanceof AccessDeniedException) {
            return error(dataFetchingEnvironment, ErrorType.FORBIDDEN, "Access denied", ACCESS_DENIED_ERROR_CODE);
        }

        if (throwable instanceof IllegalArgumentException) {
            return error(
                dataFetchingEnvironment, ErrorType.BAD_REQUEST, throwable.getMessage(), BAD_REQUEST_ERROR_CODE);
        }

        return null;
    }

    private static boolean isHubField(DataFetchingEnvironment dataFetchingEnvironment) {
        Field field = dataFetchingEnvironment.getField();

        if (field != null) {
            String fieldName = field.getName();

            if (fieldName.startsWith("aiHub") || fieldName.contains("AiHub")) {
                return true;
            }
        }

        GraphQLType parentType = dataFetchingEnvironment.getParentType();

        return parentType instanceof GraphQLNamedType namedType && namedType.getName()
            .startsWith("AiHub");
    }

    private static GraphQLError error(
        DataFetchingEnvironment dataFetchingEnvironment, ErrorClassification errorClassification, String message,
        String errorCode) {

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .message(message)
            .errorType(errorClassification)
            .extensions(Map.of("errorCode", errorCode))
            .build();
    }
}
