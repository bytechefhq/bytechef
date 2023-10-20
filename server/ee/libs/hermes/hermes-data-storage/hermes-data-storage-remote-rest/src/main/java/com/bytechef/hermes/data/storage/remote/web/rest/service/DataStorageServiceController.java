
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

package com.bytechef.hermes.data.storage.remote.web.rest.service;

import com.bytechef.hermes.component.Context.DataStorageScope;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal/data-storage-service")
public class DataStorageServiceController {

    private final DataStorageService dataStorageService;

    @SuppressFBWarnings("EI")
    public DataStorageServiceController(DataStorageService dataStorageService) {
        this.dataStorageService = dataStorageService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-value/{scope}/{scopeId}/{key}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Object> fetchValue(
        @PathVariable DataStorageScope scope, @PathVariable long scopeId, @PathVariable String key) {

        return ResponseEntity.ok(
            dataStorageService.fetchValue(scope, scopeId, key)
                .orElse(null));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/save/{scope}/{scopeId}/{key}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> save(
        @PathVariable DataStorageScope scope, @PathVariable long scopeId, @PathVariable String key,
        @RequestBody Object value) {

        dataStorageService.save(scope, scopeId, key, value);

        return ResponseEntity.noContent()
            .build();
    }
}
