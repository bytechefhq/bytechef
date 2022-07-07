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

package com.bytechef.hermes.descriptor.ext.repository;

import com.bytechef.hermes.descriptor.ext.domain.DescriptorExtHandler;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class DescriptorExtHandlerRepositoryIntTest {

    @Autowired
    private DescriptorExtHandlerRepository descriptorExtHandlerRepository;

    @BeforeEach
    public void beforeEach() {
        List<DescriptorExtHandler> descriptorExtHandlers = descriptorExtHandlerRepository.findAll();

        for (DescriptorExtHandler descriptorExtHandler : descriptorExtHandlers) {
            descriptorExtHandlerRepository.delete(descriptorExtHandler.getName());
        }
    }

    @Test
    public void testCreate() {
        DescriptorExtHandler descriptorExtHandler = getExtDescriptorHandler();

        descriptorExtHandlerRepository.create(descriptorExtHandler);

        Assertions.assertEquals(
                descriptorExtHandler, descriptorExtHandlerRepository.findByName(descriptorExtHandler.getName()));
    }

    @Test
    public void testDelete() {
        DescriptorExtHandler descriptorExtHandler = getExtDescriptorHandler();

        descriptorExtHandlerRepository.create(descriptorExtHandler);

        Assertions.assertEquals(1, descriptorExtHandlerRepository.findAll().size());

        descriptorExtHandlerRepository.delete(descriptorExtHandler.getName());

        Assertions.assertEquals(0, descriptorExtHandlerRepository.findAll().size());
    }

    @Test
    public void testFindAll() {
        DescriptorExtHandler descriptorExtHandler = getExtDescriptorHandler();

        descriptorExtHandlerRepository.create(descriptorExtHandler);

        Assertions.assertEquals(1, descriptorExtHandlerRepository.findAll().size());
    }

    @Test
    public void testFindByName() {
        DescriptorExtHandler descriptorExtHandler = getExtDescriptorHandler();

        descriptorExtHandlerRepository.create(descriptorExtHandler);

        Assertions.assertEquals(
                descriptorExtHandler, descriptorExtHandlerRepository.findByName(descriptorExtHandler.getName()));
    }

    @Test
    public void testMerge() {
        DescriptorExtHandler descriptorExtHandler = getExtDescriptorHandler();

        descriptorExtHandlerRepository.create(descriptorExtHandler);

        descriptorExtHandler.setAuthenticationExists(false);
        descriptorExtHandler.setVersions(Set.of(1d, 2d, 3d, 4d));
        descriptorExtHandler.setType("type2");
        descriptorExtHandler.setProperties(Map.of("key2", "value2"));
        descriptorExtHandler.setUpdateTime(new Date());

        descriptorExtHandlerRepository.merge(descriptorExtHandler);

        DescriptorExtHandler updatedDescriptorExtHandler = descriptorExtHandlerRepository.findByName("name");

        Assertions.assertEquals(
                descriptorExtHandler.isAuthenticationExists(), updatedDescriptorExtHandler.isAuthenticationExists());
        Assertions.assertEquals(descriptorExtHandler.getVersions(), updatedDescriptorExtHandler.getVersions());
        Assertions.assertEquals(descriptorExtHandler.getType(), updatedDescriptorExtHandler.getType());
        Assertions.assertEquals(descriptorExtHandler.getProperties(), updatedDescriptorExtHandler.getProperties());
        Assertions.assertEquals(descriptorExtHandler.getUpdateTime(), updatedDescriptorExtHandler.getUpdateTime());
    }

    private static DescriptorExtHandler getExtDescriptorHandler() {
        DescriptorExtHandler descriptorExtHandler = new DescriptorExtHandler();

        descriptorExtHandler.setName("name");
        descriptorExtHandler.setAuthenticationExists(true);
        descriptorExtHandler.setVersions(Set.of(1d, 2d));
        descriptorExtHandler.setType("type");
        descriptorExtHandler.setProperties(Map.of("key1", "value1"));
        descriptorExtHandler.setCreateTime(new Date());
        descriptorExtHandler.setUpdateTime(new Date());

        return descriptorExtHandler;
    }
}
