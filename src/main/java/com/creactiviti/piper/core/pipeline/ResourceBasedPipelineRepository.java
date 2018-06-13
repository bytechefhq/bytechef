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
 */
package com.creactiviti.piper.core.pipeline;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.google.common.base.Throwables;

public class ResourceBasedPipelineRepository extends YamlPipelineRepository {
  
  private String locationPattern = "classpath:pipelines/**/*.yaml";
  
  private static final String PREFIX = "pipelines/";
  
  public ResourceBasedPipelineRepository() {}
  
  public ResourceBasedPipelineRepository(String aLocationPattern) {
    locationPattern = aLocationPattern;
  }

  @Override
  public List<Pipeline> findAll () {
    try {
      ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources(locationPattern);
      return Arrays.asList(resources)
                   .stream()
                   .map(r -> read(r))
                   .collect(Collectors.toList());
    }
    catch(IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private Pipeline read (Resource aResource) {
    try {
      String uri = aResource.getURI().toString();
      String id = uri.substring(uri.lastIndexOf(PREFIX)+PREFIX.length(),uri.lastIndexOf('.'));
      return parsePipeline(new IdentifiableResource(id, aResource));
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public Pipeline findOne (String aId) {
    List<Pipeline> pipelines = findAll ();
    Optional<Pipeline> findFirst = pipelines.stream().filter(p->p.getId().equals(aId)).findFirst();
    if(findFirst.isPresent()) {
      return findFirst.get();
    }
    throw new IllegalArgumentException("Unknown pipeline: " + aId);
  }

}
