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

import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandler;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandlerResolver;
import com.bytechef.hermes.descriptor.model.DSL;
import java.util.List;
import org.junit.jupiter.api.Assertions;
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
@WebMvcTest(AuthenticationDescriptorController.class)
public class AuthenticationDescriptorControllerTest {

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @MockBean
    private AuthenticationDescriptorHandlerResolver authenticationDescriptorHandlerResolver;

    private static final List<AuthenticationDescriptorHandler> AUTHENTICATION_DESCRIPTOR_HANDLERS = List.of(
            () -> DSL.createAuthenticationDescriptors("task1", List.of(DSL.createAuthenticationDescriptor("auth1"))),
            () -> DSL.createAuthenticationDescriptors("task2", List.of(DSL.createAuthenticationDescriptor("auth2"))));

    @Test
    public void testGetAuthenticationDescriptor() {
        Mockito.doReturn(AUTHENTICATION_DESCRIPTOR_HANDLERS.get(0))
                .when(authenticationDescriptorHandlerResolver)
                .resolve(Mockito.anyString());

        try {
            mockMvc.perform(get("/authentication-descriptors/task1").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(
                            content()
                                    .json(
                                            """
                                       {"name":"task1","authenticationDescriptors":[{"name":"auth1"}]}
                        """));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetAuthenticationDescriptors() {
        Mockito.doReturn(AUTHENTICATION_DESCRIPTOR_HANDLERS)
                .when(authenticationDescriptorHandlerResolver)
                .getAuthenticationDescriptorHandlers();

        try {
            mockMvc.perform(get("/authentication-descriptors").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(
                            content()
                                    .json(
                                            """
                        [
                            {"name":"task1","authenticationDescriptors":[{"name":"auth1"}]},
                            {"name":"task2","authenticationDescriptors":[{"name":"auth2"}]}
                        ]
                        """));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }
}
