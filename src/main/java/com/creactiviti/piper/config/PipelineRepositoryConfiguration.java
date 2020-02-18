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
package com.creactiviti.piper.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import com.creactiviti.piper.core.pipeline.GitPipelineRepository;
import com.creactiviti.piper.core.pipeline.PipelineRepository;
import com.creactiviti.piper.core.pipeline.PipelineRepositoryChain;
import com.creactiviti.piper.core.pipeline.ResourceBasedPipelineRepository;

@Configuration
@EnableConfigurationProperties(PiperProperties.class)
public class PipelineRepositoryConfiguration {

  @Bean
  @Primary
  PipelineRepositoryChain pipelineRepository (List<PipelineRepository> aRepositories) {
    return new PipelineRepositoryChain(aRepositories);
  }
     
  @Bean
  @Order(1)
  @ConditionalOnProperty(name="piper.pipeline-repository.classpath.enabled",havingValue="true")
  ResourceBasedPipelineRepository resourceBasedPipelineRepository () {
    return new ResourceBasedPipelineRepository();
  }
  
  @Bean
  @Order(2)
  @ConditionalOnProperty(name="piper.pipeline-repository.filesystem.enabled",havingValue="true")
  ResourceBasedPipelineRepository fileSystemBasedPipelineRepository (@Value("${piper.pipeline-repository.filesystem.location-pattern}") String aBasePath) {
    return new ResourceBasedPipelineRepository(String.format("file:%s", aBasePath));
  }
  
  @Bean
  @Order(3)
  @ConditionalOnProperty(name="piper.pipeline-repository.git.enabled",havingValue="true")
  GitPipelineRepository gitPipelineRepository (PiperProperties piperProperties) {
    GitPipelineRepository gitPipelineRepository = new GitPipelineRepository(
        piperProperties.getPipelineRepository().getGit().getUrl(),
        piperProperties.getPipelineRepository().getGit().getBranch(),
        piperProperties.getPipelineRepository().getGit().getSearchPaths()
    );
    return gitPipelineRepository;
  }  
  
}
