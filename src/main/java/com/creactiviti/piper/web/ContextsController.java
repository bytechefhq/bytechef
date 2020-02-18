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
package com.creactiviti.piper.web;

import java.util.List;
import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.creactiviti.piper.core.annotations.ConditionalOnCoordinator;
import com.creactiviti.piper.core.context.Context;
import com.creactiviti.piper.core.context.ContextRepository;

@RestController
@ConditionalOnCoordinator
public class ContextsController {

  private final ContextRepository contextRepository;
  
  public ContextsController(ContextRepository aContextRepository) {
    contextRepository = Objects.requireNonNull(aContextRepository);
  }
  
  @GetMapping(value="/contexts/stack/{id}")
  public List<Context> stack (@PathVariable("id")String aContextObjectId) {
    return contextRepository.getStack(aContextObjectId);
  }
    
}
