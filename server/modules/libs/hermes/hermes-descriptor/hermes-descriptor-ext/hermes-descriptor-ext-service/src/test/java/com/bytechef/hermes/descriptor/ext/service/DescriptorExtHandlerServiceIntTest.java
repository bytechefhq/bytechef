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

package com.bytechef.hermes.descriptor.ext.service;

import com.bytechef.hermes.descriptor.ext.domain.DescriptorExtHandler;
import com.bytechef.hermes.descriptor.ext.repository.DescriptorExtHandlerRepository;
import com.bytechef.hermes.descriptor.ext.resolver.RemoteExtTaskDescriptorExtHandlerResolver;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class DescriptorExtHandlerServiceIntTest {

    @Autowired
    private DescriptorExtHandlerService descriptorExtHandlerService;

    @Autowired
    private DescriptorExtHandlerRepository descriptorExtHandlerRepository;

    @BeforeEach
    public void beforeEach() {
        for (DescriptorExtHandler descriptorExtHandler : descriptorExtHandlerRepository.findAll()) {
            descriptorExtHandlerRepository.delete(descriptorExtHandler.getName());
        }
    }

    @Test
    public void testFetchExtDescriptorHandler() {
        Optional<DescriptorExtHandler> extDescriptorHandler =
                descriptorExtHandlerService.fetchExtDescriptorHandler("csvFile");

        Assertions.assertTrue(extDescriptorHandler.isEmpty());

        descriptorExtHandlerService.save(List.of(new DescriptorExtHandler(
                "jsonFile", 1.0d, false, RemoteExtTaskDescriptorExtHandlerResolver.REMOTE, Map.of())));

        extDescriptorHandler = descriptorExtHandlerService.fetchExtDescriptorHandler("jsonFile");

        Assertions.assertTrue(extDescriptorHandler.isPresent());
    }

    @Test
    public void testGetExtDescriptorHandlers() {
        descriptorExtHandlerService.save(List.of(new DescriptorExtHandler(
                "jsonFile", 1.0d, false, RemoteExtTaskDescriptorExtHandlerResolver.REMOTE, Map.of())));

        Assertions.assertEquals(
                1,
                descriptorExtHandlerService
                        .getExtDescriptorHandlers(RemoteExtTaskDescriptorExtHandlerResolver.REMOTE)
                        .size());
    }

    @Test
    public void testSave() {
        descriptorExtHandlerService.save(List.of(new DescriptorExtHandler(
                "jsonFile", 1.0d, false, RemoteExtTaskDescriptorExtHandlerResolver.REMOTE, Map.of())));

        Optional<DescriptorExtHandler> extDescriptorHandler =
                descriptorExtHandlerService.fetchExtDescriptorHandler("jsonFile");

        Assertions.assertTrue(extDescriptorHandler.isPresent());
    }

    @Test
    public void testRemove() {
        descriptorExtHandlerService.save(List.of(new DescriptorExtHandler(
                "jsonFile", 1.0d, false, RemoteExtTaskDescriptorExtHandlerResolver.REMOTE, Map.of())));

        Optional<DescriptorExtHandler> extDescriptorHandler =
                descriptorExtHandlerService.fetchExtDescriptorHandler("jsonFile");

        Assertions.assertTrue(extDescriptorHandler.isPresent());

        descriptorExtHandlerService.remove(List.of("jsonFile"));

        extDescriptorHandler = descriptorExtHandlerService.fetchExtDescriptorHandler("jsonFile");

        Assertions.assertTrue(extDescriptorHandler.isEmpty());
    }
}
