
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

package com.bytechef.hermes.integration.facade.impl;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.facade.IntegrationFacade;
import com.bytechef.hermes.integration.service.IntegrationService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationFacadeImpl implements IntegrationFacade {

    private final IntegrationService integrationService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public IntegrationFacadeImpl(IntegrationService integrationService, TagService tagService,
        WorkflowService workflowService) {
        this.integrationService = integrationService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    public Integration addWorkflow(long id, String workflowName, String workflowDescription) {
        Integration integration = integrationService.getIntegration(id);

        Workflow workflow = workflowService.create(
            "{\"label\": \"%s\", \"description\": \"%s\", \"tasks\": []}".formatted(workflowName, workflowDescription),
            Workflow.Format.JSON, Workflow.SourceType.JDBC);

        integration.addWorkflow(workflow.getId());

        return integrationService.update(integration);
    }

    @Override
    public Integration create(
        String name, String description, String category, List<String> workflowIds, List<String> tagNames) {

        if (CollectionUtils.isEmpty(workflowIds)) {
            Workflow workflow = workflowService.create(null, Workflow.Format.JSON, Workflow.SourceType.JDBC);

            workflowIds = List.of(workflow.getId());
        }

        Set<Tag> tags = null;

        if (!CollectionUtils.isEmpty(tagNames)) {
            tags = tagService.create(new HashSet<>(tagNames));
        }

        return integrationService.create(name, description, category, new HashSet<>(workflowIds), tags);
    }

    @Override
    public void delete(Long id) {
//        Integration integration = integrationService.getIntegration(id);

        integrationService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        integration.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getIntegrationTags() {
        List<Integration> integrations = integrationService.getIntegrations();

        Set<Long> tagIds = integrations.stream()
            .map(Integration::getTagIds)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        return tagService.getTags(tagIds)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet());
    }

    @Override
    public Integration update(
        Long id, String name, String description, String category, List<String> workflowIds, List<String> tagNames) {

        Set<Tag> tags = CollectionUtils.isEmpty(tagNames) ? null : tagService.create(new HashSet<>(tagNames));
        Set<String> workflowIdSet = CollectionUtils.isEmpty(workflowIds) ? null : new HashSet<>(workflowIds);

        return integrationService.update(id, name, description, category, workflowIdSet, tags);
    }
}
