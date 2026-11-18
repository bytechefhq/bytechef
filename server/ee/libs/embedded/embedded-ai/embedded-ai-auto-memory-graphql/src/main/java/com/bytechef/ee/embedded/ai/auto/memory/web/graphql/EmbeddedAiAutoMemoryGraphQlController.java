/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.auto.memory.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemory;
import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemoryConnectedUser;
import com.bytechef.ee.embedded.ai.auto.memory.facade.EmbeddedAiAutoMemoryFacade;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryConcurrentModificationException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class EmbeddedAiAutoMemoryGraphQlController {

    private static final ErrorClassification CONFLICT = ErrorClassification.errorClassification("CONFLICT");

    private final EmbeddedAiAutoMemoryFacade embeddedAiAutoMemoryFacade;

    @SuppressFBWarnings("EI")
    public EmbeddedAiAutoMemoryGraphQlController(EmbeddedAiAutoMemoryFacade embeddedAiAutoMemoryFacade) {
        this.embeddedAiAutoMemoryFacade = embeddedAiAutoMemoryFacade;
    }

    @QueryMapping
    public List<EmbeddedAiAutoMemory> embeddedAiAutoMemories(
        @Argument int environment, @Argument @Nullable Long connectedUserId,
        @Argument @Nullable AiAutoMemoryType memoryType) {

        return embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(
            toEnvironment(environment), connectedUserId, memoryType);
    }

    @QueryMapping
    public List<EmbeddedAiAutoMemoryConnectedUser> embeddedAiAutoMemoryConnectedUsers(@Argument int environment) {
        return embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemoryConnectedUsers(toEnvironment(environment));
    }

    @MutationMapping
    public EmbeddedAiAutoMemory updateEmbeddedAiAutoMemory(@Argument UpdateEmbeddedAiAutoMemoryInput input) {
        AiAutoMemoryPatch patch = new AiAutoMemoryPatch(
            input.title(), input.description(), input.memoryType(), input.content());

        return embeddedAiAutoMemoryFacade.updateEmbeddedAiAutoMemory(
            toEnvironment(input.environment()), input.integrationInstanceId(), input.id(), input.expectedVersion(),
            patch);
    }

    @MutationMapping
    public boolean deleteEmbeddedAiAutoMemory(
        @Argument long id, @Argument long integrationInstanceId, @Argument int environment) {

        embeddedAiAutoMemoryFacade.deleteEmbeddedAiAutoMemory(toEnvironment(environment), integrationInstanceId, id);

        return true;
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "id")
    public long id(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        return aiAutoMemory.getId();
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "name")
    public String name(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        return aiAutoMemory.getName();
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "title")
    public String title(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        return aiAutoMemory.getTitle();
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "description")
    @Nullable
    public String description(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        return aiAutoMemory.getDescription();
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "memoryType")
    public String memoryType(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        AiAutoMemoryType memoryType = aiAutoMemory.getMemoryType();

        return memoryType.name();
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "content")
    public String content(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        return aiAutoMemory.getContent();
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "environmentId")
    public long environmentId(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        return aiAutoMemory.getEnvironmentOrdinal();
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "version")
    public long version(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        return aiAutoMemory.getVersion();
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "createdAt")
    @Nullable
    public Long createdAt(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        return toEpochMilli(aiAutoMemory.getCreatedAt());
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "updatedAt")
    @Nullable
    public Long updatedAt(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        return toEpochMilli(aiAutoMemory.getUpdatedAt());
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "integrationInstanceId")
    public long integrationInstanceId(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        return aiAutoMemory.getPrincipalId();
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "connectedUser")
    @Nullable
    public ConnectedUserRef connectedUser(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        Long connectedUserId = embeddedAiAutoMemory.connectedUserId();

        if (connectedUserId == null) {
            return null;
        }

        return new ConnectedUserRef(
            connectedUserId, embeddedAiAutoMemory.connectedUserName(), embeddedAiAutoMemory.connectedUserEmail(),
            embeddedAiAutoMemory.connectedUserExternalId());
    }

    @SchemaMapping(typeName = "EmbeddedAiAutoMemory", field = "integration")
    @Nullable
    public IntegrationRef integration(EmbeddedAiAutoMemory embeddedAiAutoMemory) {
        Long integrationId = embeddedAiAutoMemory.integrationId();

        if (integrationId == null) {
            return null;
        }

        return new IntegrationRef(
            integrationId, embeddedAiAutoMemory.integrationName(), embeddedAiAutoMemory.integrationComponentName());
    }

    @GraphQlExceptionHandler
    public GraphQLError handleBadRequest(
        IllegalArgumentException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(ErrorType.BAD_REQUEST)
            .message(exception.getMessage())
            .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleConflict(
        AiAutoMemoryConcurrentModificationException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(CONFLICT)
            .message("Memory '" + exception.getName() + "' was changed by someone else. Reload it and try again.")
            .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(
        AiAutoMemoryNotFoundException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(ErrorType.NOT_FOUND)
            .message(exception.getMessage())
            .build();
    }

    private static Environment toEnvironment(int environment) {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalArgumentException("Unknown environment: " + environment);
        }

        return environments[environment];
    }

    @Nullable
    private static Long toEpochMilli(@Nullable LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }

        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);

        return instant.toEpochMilli();
    }

    public record ConnectedUserRef(long id, @Nullable String name, @Nullable String email,
        @Nullable String externalId) {
    }

    public record IntegrationRef(long id, @Nullable String name, @Nullable String componentName) {
    }

    public record UpdateEmbeddedAiAutoMemoryInput(
        long id, long integrationInstanceId, int environment, long expectedVersion, @Nullable String title,
        @Nullable String description,
        @Nullable AiAutoMemoryType memoryType, @Nullable String content) {
    }
}
