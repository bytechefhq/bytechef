/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.atlas.configuration.repository.resource;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.EncodingUtils;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractResourceWorkflowRepository implements WorkflowRepository {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResourceWorkflowRepository.class);

    private final String locationPattern;
    private final String protocol;
    private final ResourcePatternResolver resourcePatternResolver;

    public AbstractResourceWorkflowRepository(
        String locationPattern, String protocol, ResourcePatternResolver resourcePatternResolver) {

        this.locationPattern = locationPattern;
        this.protocol = protocol;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @Override
    public List<Workflow> findAll() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(
                String.format("%s:%s", protocol, locationPattern));

            return Arrays.stream(resources)
                .map(this::read)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Workflow> findById(String id) {
        List<Workflow> workflows = findAll();

        return CollectionUtils.findFirst(workflows, workflow -> Objects.equals(workflow.getId(), id));
    }

    private Workflow read(Resource resource) {
        URI resourceURI;

        try {
            resourceURI = resource.getURI();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String uri = resourceURI.toString();

        String substring = uri.substring(uri.lastIndexOf("/") + 1, uri.lastIndexOf('.'));

        return readWorkflow(
            new WorkflowResource(
                EncodingUtils.encodeBase64ToString(substring), Map.of(WorkflowConstants.PATH, uri), resource,
                Workflow.Format.parse(uri)));
    }

    private static Workflow readWorkflow(WorkflowResource workflowResource) {
        try {
            return WorkflowReader.readWorkflow(workflowResource);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage());
            }
        }

        return null;
    }
}
