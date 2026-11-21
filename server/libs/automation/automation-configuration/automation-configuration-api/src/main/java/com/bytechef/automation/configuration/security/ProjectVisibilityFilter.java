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
 * Maps a {@link Project} to a {@link VisibilityRecord} and filters it through the {@link ResourceVisibilityResolver}.
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

    private void logMissingResolverOnce() {
        if (missingResolverLogged.compareAndSet(false, true)) {
            log.warn(
                "No ResourceVisibilityResolver is registered — every project is being treated as not visible. " +
                    "This application carries automation-configuration-api without a resolver implementation.");
        }
    }
}
