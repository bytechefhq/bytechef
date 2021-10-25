/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator.workflow.repository;

import com.google.common.base.Throwables;
import com.integri.atlas.engine.coordinator.workflow.Workflow;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Arik Cohen
 */
public class ResourceBasedWorkflowRepository implements WorkflowRepository {

    private String locationPattern = "classpath:workflows/**/*.yaml";
    private WorkflowMapper workflowMapper;

    private static final String PREFIX = "workflows/";

    public ResourceBasedWorkflowRepository(WorkflowMapper workflowMapper) {
        this.workflowMapper = workflowMapper;
    }

    public ResourceBasedWorkflowRepository(String aLocationPattern, WorkflowMapper workflowMapper) {
        locationPattern = aLocationPattern;
        this.workflowMapper = workflowMapper;
    }

    @Override
    public List<Workflow> findAll() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(locationPattern);
            return Arrays.asList(resources).stream().map(r -> read(r)).collect(Collectors.toList());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private Workflow read(Resource aResource) {
        try {
            String uri = aResource.getURI().toString();
            String id = uri.substring(uri.lastIndexOf(PREFIX) + PREFIX.length(), uri.lastIndexOf('.'));
            return workflowMapper.readValue(new WorkflowResource(id, aResource, WorkflowFormatType.parse(uri)));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Workflow findOne(String aId) {
        List<Workflow> workflows = findAll();
        Optional<Workflow> findFirst = workflows.stream().filter(p -> p.getId().equals(aId)).findFirst();
        if (findFirst.isPresent()) {
            return findFirst.get();
        }
        throw new IllegalArgumentException("Unknown workflow: " + aId);
    }
}
