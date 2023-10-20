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

package com.integri.atlas.task.descriptor.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.integri.atlas.task.descriptor.handler.TaskDescriptorHandler;
import com.integri.atlas.task.descriptor.model.DSL;
import com.integri.atlas.task.descriptor.model.TaskAuthDescriptor;
import com.integri.atlas.task.descriptor.model.TaskDescriptor;
import com.integri.atlas.task.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import com.integri.atlas.task.descriptor.service.TaskDescriptorHandlerService;
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
@WebMvcTest
public class TaskAuthDescriptorControllerTest {

    @MockBean
    private ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @MockBean
    private TaskDescriptorHandlerService taskDescriptorHandlerService;

    private static final List<TaskDescriptorHandler> TASK_DESCRIPTOR_HANDLERS = List.of(
        new TaskDescriptorHandler() {
            @Override
            public List<TaskAuthDescriptor> getTaskAuthDescriptors() {
                return List.of(DSL.createTaskAuthDescriptor("auth1"), DSL.createTaskAuthDescriptor("auth2"));
            }

            @Override
            public TaskDescriptor getTaskDescriptor() {
                return DSL.createTaskDescriptor("task1");
            }
        },
        new TaskDescriptorHandler() {
            @Override
            public List<TaskAuthDescriptor> getTaskAuthDescriptors() {
                return null;
            }

            @Override
            public TaskDescriptor getTaskDescriptor() {
                return DSL.createTaskDescriptor("task2");
            }
        }
    );

    @Test
    public void testGetTaskAuthDescriptor() throws Exception {
        Mockito
            .doReturn(TASK_DESCRIPTOR_HANDLERS.get(0))
            .when(taskDescriptorHandlerService)
            .getTaskDescriptorHandler(Mockito.anyString());

        mockMvc
            .perform(get("/task-auth-descriptors/task1/auth1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .json(
                        """
                        {
                            "name":"auth1"
                        }
                        """
                    )
            );
    }

    @Test
    public void testGetTaskAuthDescriptors() throws Exception {
        Mockito.doReturn(TASK_DESCRIPTOR_HANDLERS).when(taskDescriptorHandlerService).getTaskDescriptorHandlers();

        mockMvc
            .perform(get("/task-auth-descriptors").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .json(
                        """
                        [
                            {
                                "name":"auth1"
                            },
                            {
                                "name":"auth2"
                            }
                        ]
                        """
                    )
            );
    }
}
