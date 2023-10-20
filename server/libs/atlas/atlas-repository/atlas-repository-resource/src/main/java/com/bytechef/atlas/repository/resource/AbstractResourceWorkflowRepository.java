
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

package com.bytechef.atlas.repository.resource;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.atlas.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.workflow.mapper.WorkflowResource;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractResourceWorkflowRepository implements WorkflowRepository {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResourceWorkflowRepository.class);

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    private final String locationPattern;

    private static final String PREFIX = "workflows/";

    public AbstractResourceWorkflowRepository(String locationPattern) {
        this.locationPattern = locationPattern;
    }

    @Override
    public List<Workflow> findAll() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            Resource[] resources = resolver.getResources(locationPattern);

            return Arrays.stream(resources)
                .map(this::read)
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Workflow> findById(String id) {
        List<Workflow> workflows = findAll();

        return workflows.stream()
            .filter(workflow -> Objects.equals(workflow.getId(), id))
            .findFirst();
    }

    private Workflow read(Resource resource) {
        URI resourceURI;

        try {
            resourceURI = resource.getURI();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        String uri = resourceURI.toString();

        String id = ENCODER.encodeToString(
            uri.substring(
                uri.lastIndexOf(PREFIX) + PREFIX.length(), uri.lastIndexOf('.'))
                .getBytes(StandardCharsets.UTF_8));

        return readWorkflow(new WorkflowResource(id, resource, Workflow.Format.parse(uri)));
    }

    private static Workflow readWorkflow(WorkflowResource resource) {
        try {
            return WorkflowReader.readWorkflow(resource);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }
}
