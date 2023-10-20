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

package com.integri.atlas.task.definition.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.integri.atlas.task.definition.handler.TaskDefinitionHandler;
import com.integri.atlas.task.definition.model.DSL;
import com.integri.atlas.task.definition.model.TaskAuthDefinition;
import com.integri.atlas.task.definition.model.TaskDefinition;
import com.integri.atlas.task.definition.repository.ExtTaskDefinitionHandlerRepository;
import com.integri.atlas.task.definition.service.TaskDefinitionHandlerService;
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
public class TaskDefinitionControllerTest {

    @MockBean
    private ExtTaskDefinitionHandlerRepository extTaskDefinitionHandlerRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @MockBean
    private TaskDefinitionHandlerService taskDefinitionHandlerService;

    private static final List<TaskDefinitionHandler> TASK_DEFINITION_HANDLERS = List.of(
        new TaskDefinitionHandler() {
            @Override
            public List<TaskAuthDefinition> getTaskAuthDefinitions() {
                return null;
            }

            @Override
            public TaskDefinition getTaskDefinition() {
                return DSL.createTaskDefinition("task1");
            }
        },
        new TaskDefinitionHandler() {
            @Override
            public List<TaskAuthDefinition> getTaskAuthDefinitions() {
                return null;
            }

            @Override
            public TaskDefinition getTaskDefinition() {
                return DSL.createTaskDefinition("task2");
            }
        }
    );

    @Test
    public void testGetTaskDefinition() throws Exception {
        Mockito
            .doReturn(TASK_DEFINITION_HANDLERS.get(0))
            .when(taskDefinitionHandlerService)
            .getTaskDefinitionHandler(Mockito.anyString());

        mockMvc
            .perform(get("/task-definitions/task1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .json(
                        """
                        {
                            "name":"task1",
                            "version":1.0
                        }
                        """
                    )
            );
    }

    @Test
    public void testGetTaskDefinitions() throws Exception {
        Mockito.doReturn(TASK_DEFINITION_HANDLERS).when(taskDefinitionHandlerService).getTaskDefinitionHandlers();

        mockMvc
            .perform(get("/task-definitions").accept(MediaType.APPLICATION_JSON))
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
                        """
                    )
            );
    }
}
