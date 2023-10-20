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

import static com.integri.atlas.task.descriptor.resolver.RemoteExtTaskDescriptorHandlerResolver.REMOTE;

import com.integri.atlas.task.descriptor.handler.TaskAuthDescriptorHandler;
import com.integri.atlas.task.descriptor.repository.ExtTaskAuthDescriptorHandlerRepository;
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
public class TaskAuthDescriptorHandlerServiceIntTest {

    @Autowired
    private TaskAuthDescriptorHandlerService taskAuthDescriptorHandlerService;

    @Autowired
    private ExtTaskAuthDescriptorHandlerRepository extTaskAuthDescriptorHandlerRepository;

    @BeforeEach
    public void beforeEach() {
        for (Map.Entry<String, String> entry : extTaskAuthDescriptorHandlerRepository.findAll().entrySet()) {
            extTaskAuthDescriptorHandlerRepository.delete(entry.getKey());
        }
    }

    @Test
    public void testGetTaskAuthDescriptorHandler() {
        TaskAuthDescriptorHandler taskAuthDescriptorHandler = taskAuthDescriptorHandlerService.getTaskAuthDescriptorHandler(
            "csvFile"
        );

        Assertions.assertNotNull(taskAuthDescriptorHandler);

        taskAuthDescriptorHandler = taskAuthDescriptorHandlerService.getTaskAuthDescriptorHandler("jsonFile");

        Assertions.assertNull(taskAuthDescriptorHandler);

        taskAuthDescriptorHandlerService.registerExtTaskAuthDescriptorHandler("jsonFile", REMOTE);

        taskAuthDescriptorHandler = taskAuthDescriptorHandlerService.getTaskAuthDescriptorHandler("jsonFile");

        Assertions.assertNotNull(taskAuthDescriptorHandler);
    }

    @Test
    public void testGetTaskAuthDescriptorHandlers() {
        Assertions.assertEquals(1, taskAuthDescriptorHandlerService.getTaskAuthDescriptorHandlers().size());
    }

    @Test
    public void testRegisterExtTaskAuthDescriptorHandler() {
        taskAuthDescriptorHandlerService.registerExtTaskAuthDescriptorHandler("jsonFile", REMOTE);

        TaskAuthDescriptorHandler taskAuthDescriptorHandler = taskAuthDescriptorHandlerService.getTaskAuthDescriptorHandler(
            "jsonFile"
        );

        Assertions.assertNotNull(taskAuthDescriptorHandler);
    }

    @Test
    public void testUnregisterExtTaskAuthDescriptorHandler() {
        taskAuthDescriptorHandlerService.registerExtTaskAuthDescriptorHandler("jsonFile", REMOTE);

        TaskAuthDescriptorHandler taskAuthDescriptorHandler = taskAuthDescriptorHandlerService.getTaskAuthDescriptorHandler(
            "jsonFile"
        );

        Assertions.assertNotNull(taskAuthDescriptorHandler);

        taskAuthDescriptorHandlerService.unregisterExtTaskAuthDescriptorHandler("jsonFile");

        taskAuthDescriptorHandler = taskAuthDescriptorHandlerService.getTaskAuthDescriptorHandler("jsonFile");

        Assertions.assertNull(taskAuthDescriptorHandler);
    }
}
