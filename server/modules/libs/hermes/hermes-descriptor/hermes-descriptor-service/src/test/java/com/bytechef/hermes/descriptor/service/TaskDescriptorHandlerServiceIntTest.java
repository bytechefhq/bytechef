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

package com.bytechef.hermes.descriptor.service;

import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import com.bytechef.hermes.descriptor.resolver.RemoteExtTaskDescriptorHandlerResolver;
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
public class TaskDescriptorHandlerServiceIntTest {

    @Autowired
    private TaskDescriptorHandlerServiceImpl taskDescriptorHandlerService;

    @Autowired
    private ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository;

    @BeforeEach
    public void beforeEach() {
        for (Map.Entry<String, Map<String, Set<Float>>> entry :
                extTaskDescriptorHandlerRepository.findAll().entrySet()) {
            extTaskDescriptorHandlerRepository.delete(entry.getKey(), 1.0f);
        }
    }

    @Test
    public void testGetTaskDescriptorHandler() {
        TaskDescriptorHandler taskDescriptorHandler =
                taskDescriptorHandlerService.getTaskDescriptorHandler("csvFile", 1.0f);

        Assertions.assertNotNull(taskDescriptorHandler);

        taskDescriptorHandler = taskDescriptorHandlerService.getTaskDescriptorHandler("jsonFile", 1.0f);

        Assertions.assertNull(taskDescriptorHandler);

        taskDescriptorHandlerService.registerExtTaskDescriptorHandler(
                "jsonFile", 1.0f, RemoteExtTaskDescriptorHandlerResolver.REMOTE);

        taskDescriptorHandler = taskDescriptorHandlerService.getTaskDescriptorHandler("jsonFile", 1.0f);

        Assertions.assertNotNull(taskDescriptorHandler);
    }

    @Test
    public void testGetTaskDescriptorHandlers() {
        Assertions.assertEquals(
                1, taskDescriptorHandlerService.getTaskDescriptorHandlers().size());
    }

    @Test
    public void testRegisterExtTaskDescriptorHandler() {
        taskDescriptorHandlerService.registerExtTaskDescriptorHandler(
                "jsonFile", 1.0f, RemoteExtTaskDescriptorHandlerResolver.REMOTE);

        TaskDescriptorHandler taskDescriptorHandler =
                taskDescriptorHandlerService.getTaskDescriptorHandler("jsonFile", 1.0f);

        Assertions.assertNotNull(taskDescriptorHandler);
    }

    @Test
    public void testUnregisterExtTaskDescriptorHandler() {
        taskDescriptorHandlerService.registerExtTaskDescriptorHandler(
                "jsonFile", 1.0f, RemoteExtTaskDescriptorHandlerResolver.REMOTE);

        TaskDescriptorHandler taskDescriptorHandler =
                taskDescriptorHandlerService.getTaskDescriptorHandler("jsonFile", 1.0f);

        Assertions.assertNotNull(taskDescriptorHandler);

        taskDescriptorHandlerService.unregisterExtTaskDescriptorHandler("jsonFile", 1.0f);

        taskDescriptorHandler = taskDescriptorHandlerService.getTaskDescriptorHandler("jsonFile", 1.0f);

        Assertions.assertNull(taskDescriptorHandler);
    }
}
