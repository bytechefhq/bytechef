
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
import com.bytechef.atlas.repository.workflow.mapper.WorkflowMapper;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowResource;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractResourceWorkflowRepository implements WorkflowRepository {

    private String locationPattern;
    private WorkflowMapper workflowMapper;

    private static final String PREFIX = "workflows/";

    public AbstractResourceWorkflowRepository(String locationPattern, WorkflowMapper workflowMapper) {
        this.locationPattern = locationPattern;
        this.workflowMapper = workflowMapper;
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

    private Workflow read(Resource resource) {
        try {
            URI resourceURI = resource.getURI();
            String uri = resourceURI.toString();
            String id = uri.substring(uri.lastIndexOf(PREFIX) + PREFIX.length(), uri.lastIndexOf('.'));

            return workflowMapper.readValue(new WorkflowResource(id, resource, Workflow.Format.parse(uri)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Workflow> findById(String id) {
        List<Workflow> workflows = findAll();

        return workflows.stream()
            .filter(workflow -> workflow.getId()
                .equals(id))
            .findFirst();
    }
}
