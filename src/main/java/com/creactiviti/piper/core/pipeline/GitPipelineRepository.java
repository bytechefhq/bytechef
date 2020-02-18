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

import java.util.List;
import java.util.stream.Collectors;

import com.creactiviti.piper.core.git.GitOperations;
import com.creactiviti.piper.core.git.JGitTemplate;

public class GitPipelineRepository extends YamlPipelineRepository  {

  private final GitOperations git;

  public GitPipelineRepository (GitOperations aGitOperations) {
    git = aGitOperations;
  }
  
  public GitPipelineRepository (String aUrl, String aBranch, String[] aSearchPaths) {
    git = new JGitTemplate(aUrl,aBranch,aSearchPaths);
  }
  
  @Override
  public List<Pipeline> findAll () {
    synchronized(this) {
      List<IdentifiableResource> resources = git.getHeadFiles();
      List<Pipeline> pipelines = resources.stream()
                                          .map(r -> parsePipeline(r))
                                          .collect(Collectors.toList());
      return pipelines;
    }
  }

  @Override
  public Pipeline findOne (String aId) {
    synchronized(this) {
      IdentifiableResource resource = git.getFile(aId);
      return parsePipeline(resource);
    }
  }

}
