/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.web.rest;

import com.bytechef.atlas.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.cache.Clearable;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class CacheController {

    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);

    private final List<Clearable> clearables;

    public CacheController(@Autowired(required = false) List<Clearable> clearables) {
        this.clearables = clearables == null ? Collections.emptyList() : clearables;
    }

    @RequestMapping(
            value = "/caches/clear",
            method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> clear() {
        for (Clearable clearable : clearables) {
            logger.info("Clearing: {}", clearable.getClass().getName());

            clearable.clear();
        }

        return ResponseEntity.ok().build();
    }
}
