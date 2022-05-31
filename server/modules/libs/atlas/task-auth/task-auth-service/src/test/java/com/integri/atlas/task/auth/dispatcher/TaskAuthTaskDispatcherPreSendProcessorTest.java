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

package com.integri.atlas.task.auth.dispatcher;

import static org.mockito.Mockito.doReturn;

import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import com.integri.atlas.task.auth.SimpleTaskAuth;
import com.integri.atlas.task.auth.service.TaskAuthService;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(SpringExtension.class)
public class TaskAuthTaskDispatcherPreSendProcessorTest {

    @MockBean
    TaskAuthService taskAuthService;

    @Test
    public void testProcess() {
        SimpleTaskAuth taskAuth = getSimpleTaskAuth();

        doReturn(taskAuth).when(taskAuthService).getTaskAuth(taskAuth.getId());

        TaskAuthTaskDispatcherPreSendProcessor taskAuthTaskDispatcherPreSendProcessor = new TaskAuthTaskDispatcherPreSendProcessor(
            taskAuthService
        );

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put(Constants.AUTH, Map.of(Constants.ID, taskAuth.getId()));

        TaskExecution processedTaskExecution = taskAuthTaskDispatcherPreSendProcessor.process(taskExecution);

        Assertions.assertEquals(taskAuth, processedTaskExecution.get(Constants.AUTH));
    }

    private static SimpleTaskAuth getSimpleTaskAuth() {
        SimpleTaskAuth taskAuth = new SimpleTaskAuth();

        taskAuth.setName("name");
        taskAuth.setId(UUIDGenerator.generate());
        taskAuth.setCreateTime(new Date());
        taskAuth.setProperties(Map.of("key1", "value1"));
        taskAuth.setUpdateTime(new Date());
        taskAuth.setType("type");

        return taskAuth;
    }
}
