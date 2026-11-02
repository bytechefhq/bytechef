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

package com.bytechef.automation.ai.auto.memory.web.graphql;

import com.bytechef.automation.ai.auto.memory.domain.AiAutoMemoryPrincipal;
import com.bytechef.automation.ai.auto.memory.domain.AiAutoMemoryPrincipalKey;
import com.bytechef.automation.ai.auto.memory.facade.AiAutoMemoryFacade;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryConcurrentModificationException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
public class AiAutoMemoryGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(AiAutoMemoryGraphQlController.class);

    static final ErrorClassification CONFLICT = ErrorClassification.errorClassification("CONFLICT");

    private final AiAutoMemoryFacade aiAutoMemoryFacade;

    @SuppressFBWarnings("EI")
    public AiAutoMemoryGraphQlController(AiAutoMemoryFacade aiAutoMemoryFacade) {
        this.aiAutoMemoryFacade = aiAutoMemoryFacade;
    }

    @QueryMapping
    public List<AiAutoMemory> aiAutoMemories(
        @Argument long workspaceId, @Argument int environment, @Argument @Nullable AiAutoMemoryType memoryType,
        @Argument @Nullable AiAutoMemoryPrincipalKey principal) {

        return aiAutoMemoryFacade.getAiAutoMemories(workspaceId, toEnvironment(environment), memoryType, principal);
    }

    @QueryMapping
    @Nullable
    public AiAutoMemory aiAutoMemory(
        @Argument long workspaceId, @Argument long id, @Argument int environment,
        @Argument @Nullable AiAutoMemoryPrincipalKey principal) {

        Optional<AiAutoMemory> aiAutoMemoryOptional = aiAutoMemoryFacade.fetchAiAutoMemory(
            workspaceId, toEnvironment(environment), id, principal);

        return aiAutoMemoryOptional.orElse(null);
    }

    @QueryMapping
    public List<AiAutoMemoryPrincipal> aiAutoMemoryPrincipals(@Argument long workspaceId, @Argument int environment) {
        return aiAutoMemoryFacade.getAiAutoMemoryPrincipals(workspaceId, toEnvironment(environment));
    }

    @MutationMapping
    public AiAutoMemory updateAiAutoMemory(@Argument UpdateAiAutoMemoryInput input) {
        AiAutoMemoryPatch patch = new AiAutoMemoryPatch(
            input.title(), input.description(), input.memoryType(), input.content());

        return aiAutoMemoryFacade.updateAiAutoMemory(
            input.workspaceId(), toEnvironment(input.environment()), input.id(), input.expectedVersion(), patch,
            input.principal());
    }

    @MutationMapping
    public boolean deleteAiAutoMemory(
        @Argument long workspaceId, @Argument long id, @Argument int environment,
        @Argument @Nullable AiAutoMemoryPrincipalKey principal) {

        aiAutoMemoryFacade.deleteAiAutoMemory(workspaceId, toEnvironment(environment), id, principal);

        return true;
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "principalId")
    public long principalId(AiAutoMemory memory) {
        return memory.getPrincipalId();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "principalType")
    public String principalType(AiAutoMemory memory) {
        return memory.getPrincipalType()
            .name();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "memoryType")
    public String memoryType(AiAutoMemory memory) {
        return memory.getMemoryType()
            .name();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "createdAt")
    @Nullable
    public Long createdAt(AiAutoMemory memory) {
        return memory.getCreatedAt() == null ? null
            : memory.getCreatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "environmentId")
    public long environmentId(AiAutoMemory memory) {
        return memory.getEnvironmentOrdinal();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "updatedAt")
    @Nullable
    public Long updatedAt(AiAutoMemory memory) {
        return memory.getUpdatedAt() == null ? null
            : memory.getUpdatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(
        AiAutoMemoryNotFoundException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(ErrorType.NOT_FOUND)
            .message(exception.getMessage())
            .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleConflict(
        AiAutoMemoryConcurrentModificationException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        logRejection(dataFetchingEnvironment, exception);

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(CONFLICT)
            .message("Memory '" + exception.getName() + "' was changed by someone else. Reload it and try again.")
            .build();
    }

    @GraphQlExceptionHandler(IllegalArgumentException.class)
    public GraphQLError handleBadRequest(
        IllegalArgumentException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        Field field = dataFetchingEnvironment.getField();

        log.warn("Rejected auto-memory request {}", field.getName(), exception);

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(ErrorType.BAD_REQUEST)
            .message(exception.getMessage())
            .build();
    }

    private static void logRejection(DataFetchingEnvironment dataFetchingEnvironment, RuntimeException exception) {
        if (log.isDebugEnabled()) {
            Field field = dataFetchingEnvironment.getField();

            log.debug("Rejected auto-memory request {}", field.getName(), exception);
        }
    }

    private static Environment toEnvironment(int environment) {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalArgumentException("Unknown environment: " + environment);
        }

        return environments[environment];
    }

    public record UpdateAiAutoMemoryInput(
        long id, long workspaceId, int environment, long expectedVersion, @Nullable String title,
        @Nullable String description, @Nullable AiAutoMemoryType memoryType, @Nullable String content,
        @Nullable AiAutoMemoryPrincipalKey principal) {
    }
}
