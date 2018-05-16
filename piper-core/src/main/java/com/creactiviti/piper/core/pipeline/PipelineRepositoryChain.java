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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.util.Assert;

import com.creactiviti.piper.core.cache.Clearable;

/**
 * @author Arik Cohen
 * @since Jun 2, 2017
 */
public class PipelineRepositoryChain implements PipelineRepository, Clearable {

  private final List<PipelineRepository> repositories;

  private CacheManager cacheManager = new ConcurrentMapCacheManager();
  
  private static final String CACHE_ALL = GitPipelineRepository.class.getName()+".all";
  private static final String CACHE_ONE = GitPipelineRepository.class.getName()+".one";
  
  private Logger logger = LoggerFactory.getLogger(getClass());

  public PipelineRepositoryChain(List<PipelineRepository> aRepositories) {
    Assert.notNull(aRepositories, "'aRepositories' can not be null");
    repositories = aRepositories;
  }

  @Override
  public Pipeline findOne(String aId) {
    Cache oneCache = cacheManager.getCache(CACHE_ONE);
    if(oneCache.get(aId)!=null) {
      return (Pipeline) oneCache.get(aId).get();
    }
    Cache allCache = cacheManager.getCache(CACHE_ALL);
    if(allCache.get(CACHE_ALL) != null) {
      List<Pipeline> pipelines = (List<Pipeline>) allCache.get(CACHE_ALL).get();
      for(Pipeline p : pipelines) {
        if(p.getId().equals(aId)) {
          return p;
        }
      }
    }
    for(PipelineRepository repository : repositories) {
      try {
        Pipeline pipeline = repository.findOne(aId);
        oneCache.put(aId, pipeline);
        return pipeline;
      }
      catch (Exception e) {
        logger.debug("{}",e.getMessage());
      }
    }
    throw new IllegalArgumentException("Unknown pipeline: " + aId);
  }

  @Override
  public List<Pipeline> findAll() {
    Cache cache = cacheManager.getCache(CACHE_ALL);
    if(cache.get(CACHE_ALL) != null) {
      return (List<Pipeline>) cache.get(CACHE_ALL).get();
    }
    List<Pipeline> pipelines = repositories.stream()
                                           .map(r->r.findAll())
                                           .flatMap(List::stream)
                                           .sorted((a,b)->a.getLabel().compareTo(b.getLabel()))
                                           .collect(Collectors.toList());
    cache.put(CACHE_ALL, pipelines);
    return pipelines;
  }

  @Override
  public void clear() {
    cacheManager.getCacheNames()
                .stream()
                .forEach(c->cacheManager.getCache(c).clear());
  }

}
