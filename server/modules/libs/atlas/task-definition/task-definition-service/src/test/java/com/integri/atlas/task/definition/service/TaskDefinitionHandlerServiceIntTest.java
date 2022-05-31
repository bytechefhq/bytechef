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

package com.integri.atlas.task.definition.service;

import static com.integri.atlas.task.definition.resolver.RemoteExtTaskDefinitionHandlerResolver.*;

import com.integri.atlas.task.definition.handler.TaskDefinitionHandler;
import com.integri.atlas.task.definition.repository.ExtTaskDefinitionHandlerRepository;
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
public class TaskDefinitionHandlerServiceIntTest {

    @Autowired
    private TaskDefinitionHandlerService taskDefinitionHandlerService;

    @Autowired
    private ExtTaskDefinitionHandlerRepository extTaskDefinitionHandlerRepository;

    @BeforeEach
    public void beforeEach() {
        for (Map.Entry<String, String> entry : extTaskDefinitionHandlerRepository.findAll().entrySet()) {
            extTaskDefinitionHandlerRepository.delete(entry.getKey());
        }
    }

    @Test
    public void testGetTaskDefinitionHandler() {
        TaskDefinitionHandler taskDefinitionHandler = taskDefinitionHandlerService.getTaskDefinitionHandler("memory");

        Assertions.assertNotNull(taskDefinitionHandler);

        taskDefinitionHandler = taskDefinitionHandlerService.getTaskDefinitionHandler("remote");

        Assertions.assertNull(taskDefinitionHandler);

        taskDefinitionHandlerService.registerExtTaskDefinitionHandler("remote", REMOTE);

        taskDefinitionHandler = taskDefinitionHandlerService.getTaskDefinitionHandler("remote");

        Assertions.assertNotNull(taskDefinitionHandler);
    }

    @Test
    public void testGetTaskDefinitionHandlers() {
        Assertions.assertEquals(1, taskDefinitionHandlerService.getTaskDefinitionHandlers().size());
    }

    @Test
    public void testRegisterExtTaskDefinitionHandler() {
        taskDefinitionHandlerService.registerExtTaskDefinitionHandler("remote", REMOTE);

        TaskDefinitionHandler taskDefinitionHandler = taskDefinitionHandlerService.getTaskDefinitionHandler("remote");

        Assertions.assertNotNull(taskDefinitionHandler);
    }

    @Test
    public void testUnregisterExtTaskDefinitionHandler() {
        taskDefinitionHandlerService.registerExtTaskDefinitionHandler("remote", REMOTE);

        TaskDefinitionHandler taskDefinitionHandler = taskDefinitionHandlerService.getTaskDefinitionHandler("remote");

        Assertions.assertNotNull(taskDefinitionHandler);

        taskDefinitionHandlerService.unregisterExtTaskDefinitionHandler("remote");

        taskDefinitionHandler = taskDefinitionHandlerService.getTaskDefinitionHandler("remote");

        Assertions.assertNull(taskDefinitionHandler);
    }
}
