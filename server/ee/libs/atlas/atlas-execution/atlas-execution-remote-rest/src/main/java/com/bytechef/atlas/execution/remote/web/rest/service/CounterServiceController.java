
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

package com.bytechef.atlas.execution.remote.web.rest.service;

import com.bytechef.atlas.execution.service.CounterService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
public class CounterServiceController {

    private final CounterService counterService;

    @SuppressFBWarnings("EI")
    public CounterServiceController(CounterService counterService) {
        this.counterService = counterService;
    }

    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/counter-service/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        counterService.delete(id);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/counter-service/decrement/{id}")
    public ResponseEntity<Void> decrement(@PathVariable long id) {
        counterService.decrement(id);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/counter-service/set/{id}/{value}")
    public ResponseEntity<Void> set(@PathVariable long id, @PathVariable long value) {
        counterService.set(id, value);

        return ResponseEntity.noContent()
            .build();
    }
}
