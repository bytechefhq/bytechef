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

package com.bytechef.automation.knowledgebase.web.graphql.dto;

import com.bytechef.automation.knowledgebase.dto.CreateKnowledgeBaseSourceInput;
import com.bytechef.platform.knowledgebase.domain.TombstoneStrategy;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * GraphQL input shape for creating a Knowledge Base source. Mirrors the schema's {@code
 * CreateKnowledgeBaseSourceInput}; a controller adapter translates this to the facade-level
 * {@link com.bytechef.automation.knowledgebase.dto.CreateKnowledgeBaseSourceInput}.
 *
 * <p>
 * {@code workspaceId} is read off the GraphQL input by the controller and passed as the first argument to
 * {@link com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseSourceFacade#create}; it is not part of the
 * facade-level DTO.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record CreateKnowledgeBaseSourceGraphQlInput(
    Long workspaceId,
    @Nullable Long environmentId,
    Long knowledgeBaseId,
    String name,
    String sourceComponentName,
    int sourceComponentVersion,
    @Nullable String sourceClusterElementName,
    @Nullable Long connectionId,
    String cadence,
    @Nullable Map<String, ?> parameters,
    @Nullable Map<String, ?> metadataFields,
    @Nullable String fullReplaceCadence,
    @Nullable TombstoneStrategy tombstoneStrategy) {

    public CreateKnowledgeBaseSourceInput toFacadeInput() {
        return new CreateKnowledgeBaseSourceInput(
            name, sourceComponentName, sourceComponentVersion, sourceClusterElementName, connectionId,
            knowledgeBaseId, cadence, parameters, metadataFields, fullReplaceCadence, tombstoneStrategy);
    }
}
