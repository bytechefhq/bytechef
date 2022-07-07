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

package com.bytechef.hermes.descriptor.ext.web.rest;

import com.bytechef.hermes.descriptor.ext.domain.DescriptorExtHandler;
import com.bytechef.hermes.descriptor.ext.service.DescriptorExtHandlerService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
public class DescriptorExtHandlerController {

    private final DescriptorExtHandlerService descriptorExtHandlerService;

    public DescriptorExtHandlerController(DescriptorExtHandlerService descriptorExtHandlerService) {
        this.descriptorExtHandlerService = descriptorExtHandlerService;
    }

    @DeleteMapping(value = "/descriptor-ext-handlers/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> removeExtDescriptorHandlers(@RequestBody List<String> names) {
        descriptorExtHandlerService.remove(names);

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/descriptor-ext-handlers/save", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> saveExtDescriptorHandlers(
            @RequestBody List<DescriptorExtHandler> descriptorExtHandlers) {

        descriptorExtHandlerService.save(descriptorExtHandlers);

        return ResponseEntity.ok().build();
    }
}
