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

package com.integri.atlas.task.auth.web.rest;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentCaptor.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.encryption.EncryptionKey;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import com.integri.atlas.task.auth.SimpleTaskAuth;
import com.integri.atlas.task.auth.repository.TaskAuthRepository;
import com.integri.atlas.task.auth.service.TaskAuthService;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
@WebMvcTest(TaskAuthController.class)
public class TaskAuthControllerTest {

    @MockBean
    private EncryptionKey encryptionKey;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskAuthRepository taskAuthRepository;

    @SpyBean
    private TaskAuthService taskAuthService;

    @Test
    public void testDeleteTaskAuth() throws Exception {
        this.mockMvc.perform(delete("/task-auths/1")).andExpect(status().isOk());

        ArgumentCaptor<String> argument = forClass(String.class);

        verify(taskAuthService).delete(argument.capture());

        Assertions.assertEquals("1", argument.getValue());
    }

    @Test
    public void testGetTaskAuth() throws Exception {
        SimpleTaskAuth taskAuth = getSimpleTaskAuth();

        doReturn(taskAuth).when(taskAuthService).getTaskAuth(anyString());

        this.mockMvc.perform(get("/task-auths/1"))
            .andExpect(status().isOk())
            .andExpect(content().json(objectMapper.writeValueAsString(taskAuth)));
    }

    @Test
    public void testGetTaskAuths() throws Exception {
        SimpleTaskAuth taskAuth = getSimpleTaskAuth();

        doReturn(List.of(taskAuth)).when(taskAuthService).getTaskAuths();

        this.mockMvc.perform(get("/task-auths"))
            .andExpect(status().isOk())
            .andExpect(content().json(objectMapper.writeValueAsString(List.of(taskAuth))));
    }

    @Test
    public void testPostTaskAuth() throws Exception {
        SimpleTaskAuth taskAuth = getSimpleTaskAuth();

        doReturn(taskAuth).when(taskAuthService).create(anyString(), anyString(), any());

        this.mockMvc.perform(
                post("/task-auths")
                    .content(objectMapper.writeValueAsString(taskAuth))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(taskAuth.getId())))
            .andExpect(jsonPath("$.name", is(taskAuth.getName())))
            .andExpect(jsonPath("$.properties", is(taskAuth.getProperties())))
            .andExpect(jsonPath("$.type", is(taskAuth.getType())));
    }

    @Test
    public void putTaskAuth() throws Exception {
        SimpleTaskAuth taskAuth = getSimpleTaskAuth();

        taskAuth.setName("name2");

        doReturn(taskAuth).when(taskAuthService).update(anyString(), anyString());

        this.mockMvc.perform(
                put("/task-auths")
                    .content(
                        """
                  {
                    "id": "1",
                    "name": "name2"
                  }
                    """
                    )
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(taskAuth.getId())))
            .andExpect(jsonPath("$.name", is("name2")));
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
