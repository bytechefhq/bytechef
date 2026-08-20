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

package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * The one place a {@link Project} is mapped to a {@link VisibilityRecord} and filtered through the
 * {@link ResourceVisibilityResolver}. Every project list surface (projects, workflows, deployments, executions, search,
 * GraphQL) goes through here so the mapping cannot drift between them.
 *
 * <p>
 * Lives in {@code -api} rather than {@code -service} because {@code automation-configuration-graphql} depends only on
 * {@code -api} — the same reason {@code ResourceVisibilityConfiguration} declares its bean in an api module.
 *
 * <p>
 * The resolver is injected as an {@link ObjectProvider} because this component sits in a module six distributed EE apps
 * carry WITHOUT {@code automation-configuration-service}, where both resolver implementations live: ai-copilot,
 * connection, coordinator, execution, webhook and worker (configuration-app is the one that carries both). Those apps
 * scan {@code com.bytechef}, so a hard dependency would kill their context at startup. Resolution is therefore per
 * call, and an absent resolver hides everything rather than showing everything — the branch is unreachable in
 * production (every app carrying a caller of this filter also carries a resolver), it exists so the context boots.
 * {@code getIfAvailable} rather than {@code getIfUnique}: exactly one implementation is ever active, since the two are
 * split by the CE/EE edition conditionals, so two candidates is a misconfiguration that should throw loudly rather than
 * silently degrade to hiding every project.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectVisibilityFilter {

    public static final String PROJECT = "Project";

    private static final Logger log = LoggerFactory.getLogger(ProjectVisibilityFilter.class);

    private final AtomicBoolean missingResolverLogged = new AtomicBoolean();
    private final ObjectProvider<ResourceVisibilityResolver> resourceVisibilityResolverProvider;

    @SuppressFBWarnings("EI")
    public ProjectVisibilityFilter(ObjectProvider<ResourceVisibilityResolver> resourceVisibilityResolverProvider) {
        this.resourceVisibilityResolverProvider = resourceVisibilityResolverProvider;
    }

    public static VisibilityRecord toVisibilityRecord(Project project) {
        return new VisibilityRecord(
            Objects.requireNonNull(project.getId(), "id"), project.getVisibility(), project.getCreatedBy());
    }

    /**
     * Ids of the given projects the current principal may see. The resolver ignores {@code workspaceId} (it resolves
     * against the principal), so callers without a workspace in hand pass the projects as-is. Returns no ids at all
     * when no resolver is registered.
     */
    public Set<Long> visibleProjectIds(Collection<Project> projects) {
        if (projects.isEmpty()) {
            return Set.of();
        }

        ResourceVisibilityResolver resourceVisibilityResolver = resourceVisibilityResolverProvider.getIfAvailable();

        if (resourceVisibilityResolver == null) {
            logMissingResolverOnce();

            return Set.of();
        }

        List<VisibilityRecord> visibilityRecords = projects.stream()
            .map(ProjectVisibilityFilter::toVisibilityRecord)
            .toList();

        return resourceVisibilityResolver.filterVisibleIds(PROJECT, 0L, visibilityRecords);
    }

    public List<Project> filterVisible(Collection<Project> projects) {
        Set<Long> visibleProjectIds = visibleProjectIds(projects);

        return projects.stream()
            .filter(project -> visibleProjectIds.contains(project.getId()))
            .toList();
    }

    /**
     * Warns once per instance rather than per call: this runs on list paths, where a per-call warning would flood the
     * log without adding anything to the first one.
     */
    private void logMissingResolverOnce() {
        if (missingResolverLogged.compareAndSet(false, true)) {
            log.warn(
                "No ResourceVisibilityResolver is registered — every project is being treated as not visible. " +
                    "This application carries automation-configuration-api without a resolver implementation.");
        }
    }
}
