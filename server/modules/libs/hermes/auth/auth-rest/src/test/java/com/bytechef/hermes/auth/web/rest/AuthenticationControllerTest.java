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

package com.bytechef.hermes.auth.web.rest;

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

import com.bytechef.atlas.uuid.UUIDGenerator;
import com.bytechef.hermes.auth.domain.SimpleAuthentication;
import com.bytechef.hermes.auth.repository.AuthenticationRepository;
import com.bytechef.hermes.auth.service.AuthenticationService;
import com.bytechef.hermes.encryption.EncryptionKey;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@WebMvcTest(AuthenticationController.class)
public class AuthenticationControllerTest {

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
    private AuthenticationRepository authenticationRepository;

    @SpyBean
    private AuthenticationService authenticationService;

    @Test
    public void testDeleteAuthentication() throws Exception {
        this.mockMvc.perform(delete("/authentications/1")).andExpect(status().isOk());

        ArgumentCaptor<String> argument = forClass(String.class);

        verify(authenticationService).delete(argument.capture());

        Assertions.assertEquals("1", argument.getValue());
    }

    @Test
    public void testGetAuthentication() throws Exception {
        SimpleAuthentication simpleAuthentication = getSimpleAuthentication();

        doReturn(simpleAuthentication).when(authenticationService).fetchAuthentication(anyString());

        this.mockMvc
                .perform(get("/authentications/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(simpleAuthentication)));
    }

    @Test
    public void testGetAuthentications() throws Exception {
        SimpleAuthentication simpleAuthentication = getSimpleAuthentication();

        doReturn(List.of(simpleAuthentication)).when(authenticationService).getAuthentications();

        this.mockMvc
                .perform(get("/authentications"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(simpleAuthentication))));
    }

    @Test
    public void testPostAuthentication() throws Exception {
        SimpleAuthentication authentication = getSimpleAuthentication();

        doReturn(authentication).when(authenticationService).create(anyString(), anyString(), any());

        this.mockMvc
                .perform(post("/authentications")
                        .content(objectMapper.writeValueAsString(authentication))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(authentication.getId())))
                .andExpect(jsonPath("$.name", is(authentication.getName())))
                .andExpect(jsonPath("$.properties", is(authentication.getProperties())))
                .andExpect(jsonPath("$.type", is(authentication.getType())));
    }

    @Test
    public void putAuthentication() throws Exception {
        SimpleAuthentication authentication = getSimpleAuthentication();

        authentication.setName("name2");

        doReturn(authentication).when(authenticationService).update(anyString(), anyString());

        this.mockMvc
                .perform(put("/authentications")
                        .content(
                                """
                  {
                    "id": "1",
                    "name": "name2"
                  }
                    """)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(authentication.getId())))
                .andExpect(jsonPath("$.name", is("name2")));
    }

    private static SimpleAuthentication getSimpleAuthentication() {
        SimpleAuthentication authentication = new SimpleAuthentication();

        authentication.setName("name");
        authentication.setId(UUIDGenerator.generate());
        authentication.setCreateTime(new Date());
        authentication.setProperties(Map.of("key1", "value1"));
        authentication.setUpdateTime(new Date());
        authentication.setType("type");

        return authentication;
    }
}
