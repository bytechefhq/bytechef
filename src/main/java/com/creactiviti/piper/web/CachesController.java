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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creactiviti.piper.core.annotations.ConditionalOnCoordinator;
import com.creactiviti.piper.core.cache.Clearable;

@RestController
@ConditionalOnCoordinator
public class CachesController {

  @Autowired(required=false)
  private List<Clearable> clearables = Collections.emptyList();
  
  private Logger logger = LoggerFactory.getLogger(getClass());

  @PostMapping("/caches/clear")
  public Map<String, String> clear () {
    for(Clearable c : clearables) {
      logger.info("Clearing: {}",c.getClass().getName());
      c.clear();
    }
    return Collections.singletonMap("status", "OK");
  }
}
