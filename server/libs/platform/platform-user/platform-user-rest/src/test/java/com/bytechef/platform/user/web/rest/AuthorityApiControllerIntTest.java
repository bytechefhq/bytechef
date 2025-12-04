/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.user.web.rest;

import static com.bytechef.platform.user.domain.AuthorityAsserts.assertAuthorityUpdatableFieldsEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.web.rest.config.UserIntTestConfiguration;
import com.bytechef.platform.user.web.rest.config.UserIntTestConfigurationSharedMocks;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AuthorityApiController} REST controller.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = UserIntTestConfiguration.class, properties = "bytechef.tenant.mode=single")
@AutoConfigureMockMvc
@WithMockUser(authorities = {
    "ROLE_ADMIN"
})
@UserIntTestConfigurationSharedMocks
class AuthorityApiControllerIntTest {

    private static final String ENTITY_API_URL = "/api/platform/internal/authorities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{name}";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private MockMvc restAuthorityMockMvc;

    private Authority authority;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it, if they test an entity which requires
     * the current entity.
     */
    public static Authority createEntity() {
        UUID uuid = UUID.randomUUID();

        return new Authority().name(uuid.toString());
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it, if they test an entity which requires
     * the current entity.
     */
    public static Authority createUpdatedEntity() {
        UUID uuid = UUID.randomUUID();

        return new Authority().name(uuid.toString());
    }

    @BeforeEach
    public void beforeEach() {
        authorityRepository.deleteAll();

        authority = createEntity();
    }

    @Test
    @Transactional
    void createAuthority() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();

        // Create the Authority
        var returnedAuthority = objectMapper.readValue(
            restAuthorityMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authority)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Authority.class);

        // Validate the Authority in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertAuthorityUpdatableFieldsEquals(returnedAuthority, getPersistedAuthority(returnedAuthority));
    }

    @Test
    @Transactional
    void createAuthorityWithExistingId() throws Exception {
        // Create the Authority with an existing ID
        authorityRepository.save(authority);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAuthorityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(authority)))
            .andExpect(status().isBadRequest());

        // Validate the Authority in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllAuthorities() throws Exception {
        // Initialize the database
        UUID uuid = UUID.randomUUID();

        authority.setName(uuid.toString());

        authorityRepository.save(authority);

        // Get all the authorityList
        restAuthorityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=name,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].name").value(hasItem(authority.getName())));
    }

    @Test
    @Transactional
    void getAuthority() throws Exception {
        // Initialize the database
        UUID uuid = UUID.randomUUID();

        authority.setName(uuid.toString());

        authorityRepository.save(authority);

        // Get the authority
        restAuthorityMockMvc
            .perform(get(ENTITY_API_URL_ID, authority.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.name").value(authority.getName()));
    }

    @Test
    @Transactional
    void getNonExistingAuthority() throws Exception {
        // Get the authority
        restAuthorityMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void deleteAuthority() throws Exception {
        // Initialize the database
        UUID uuid = UUID.randomUUID();

        authority.setName(uuid.toString());

        authorityRepository.save(authority);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the authority
        restAuthorityMockMvc
            .perform(
                delete(ENTITY_API_URL_ID, authority.getId()).with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return authorityRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Authority getPersistedAuthority(Authority authority) {
        return authorityRepository.findByName(authority.getName())
            .orElseThrow();
    }
}
