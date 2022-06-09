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

package com.integri.atlas.task.descriptor.service;

import static com.integri.atlas.task.descriptor.resolver.RemoteExtTaskDescriptorHandlerResolver.*;

import com.integri.atlas.task.descriptor.handler.TaskDescriptorHandler;
import com.integri.atlas.task.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import java.util.Map;
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
    private TaskDescriptorHandlerService taskDescriptorHandlerService;

    @Autowired
    private ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository;

    @BeforeEach
    public void beforeEach() {
        for (Map.Entry<String, String> entry : extTaskDescriptorHandlerRepository.findAll().entrySet()) {
            extTaskDescriptorHandlerRepository.delete(entry.getKey());
        }
    }

    @Test
    public void testGetTaskDescriptorHandler() {
        TaskDescriptorHandler taskDescriptorHandler = taskDescriptorHandlerService.getTaskDescriptorHandler("memory");

        Assertions.assertNotNull(taskDescriptorHandler);

        taskDescriptorHandler = taskDescriptorHandlerService.getTaskDescriptorHandler("remote");

        Assertions.assertNull(taskDescriptorHandler);

        taskDescriptorHandlerService.registerExtTaskDescriptorHandler("remote", REMOTE);

        taskDescriptorHandler = taskDescriptorHandlerService.getTaskDescriptorHandler("remote");

        Assertions.assertNotNull(taskDescriptorHandler);
    }

    @Test
    public void testGetTaskDescriptorHandlers() {
        Assertions.assertEquals(1, taskDescriptorHandlerService.getTaskDescriptorHandlers().size());
    }

    @Test
    public void testRegisterExtTaskDescriptorHandler() {
        taskDescriptorHandlerService.registerExtTaskDescriptorHandler("remote", REMOTE);

        TaskDescriptorHandler taskDescriptorHandler = taskDescriptorHandlerService.getTaskDescriptorHandler("remote");

        Assertions.assertNotNull(taskDescriptorHandler);
    }

    @Test
    public void testUnregisterExtTaskDescriptorHandler() {
        taskDescriptorHandlerService.registerExtTaskDescriptorHandler("remote", REMOTE);

        TaskDescriptorHandler taskDescriptorHandler = taskDescriptorHandlerService.getTaskDescriptorHandler("remote");

        Assertions.assertNotNull(taskDescriptorHandler);

        taskDescriptorHandlerService.unregisterExtTaskDescriptorHandler("remote");

        taskDescriptorHandler = taskDescriptorHandlerService.getTaskDescriptorHandler("remote");

        Assertions.assertNull(taskDescriptorHandler);
    }
}
