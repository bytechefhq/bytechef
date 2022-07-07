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

package com.bytechef.hermes.descriptor.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandlerResolver;
import com.bytechef.hermes.descriptor.model.DSL;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
@WebMvcTest(TaskDescriptorController.class)
public class TaskDescriptorControllerTest {

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @MockBean
    private TaskDescriptorHandlerResolver taskDescriptorHandlerResolver;

    private static final List<TaskDescriptorHandler> TASK_DESCRIPTOR_HANDLERS =
            List.of(() -> DSL.createTaskDescriptor("task1"), () -> DSL.createTaskDescriptor("task2"));

    @Test
    public void testGetTaskDescriptor() throws Exception {
        Mockito.doReturn(TASK_DESCRIPTOR_HANDLERS.get(0))
                .when(taskDescriptorHandlerResolver)
                .resolve(Mockito.anyString(), Mockito.anyFloat());

        mockMvc.perform(get("/task-descriptors/task1/1.0").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        """
                        {
                            "name":"task1",
                            "version":1.0
                        }
                        """));
    }

    @Test
    public void testGetTaskDescriptors() throws Exception {
        Mockito.doReturn(TASK_DESCRIPTOR_HANDLERS)
                .when(taskDescriptorHandlerResolver)
                .getTaskDescriptorHandlers();

        mockMvc.perform(get("/task-descriptors").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        """
                        [
                            {
                                "name":"task1",
                                "version":1.0
                            },
                            {
                                "name":"task2",
                                "version":1.0
                            }
                        ]
                        """));
    }
}
