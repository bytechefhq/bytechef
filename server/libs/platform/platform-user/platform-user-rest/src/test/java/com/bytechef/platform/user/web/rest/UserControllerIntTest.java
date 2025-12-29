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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.mapper.UserMapper;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.web.rest.config.UserIntTestConfiguration;
import com.bytechef.platform.user.web.rest.config.UserIntTestConfigurationSharedMocks;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link UserController} REST controller.
 *
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = UserIntTestConfiguration.class, properties = "bytechef.tenant.mode=single")
@WithMockUser(authorities = AuthorityConstants.ADMIN)
@UserIntTestConfigurationSharedMocks
class UserControllerIntTest {

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final String UPDATED_LOGIN = "bytechef";

    private static final Long DEFAULT_ID = 1L;

    private static final String DEFAULT_ID_STR = "1";

    private static final String DEFAULT_EMAIL = "johndoe@localhost.com";
    private static final String UPDATED_EMAIL = "bytechef@localhost.com";

    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String UPDATED_FIRSTNAME = "firstName";

    private static final String DEFAULT_LASTNAME = "doe";
    private static final String UPDATED_LASTNAME = "lastName";

    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String UPDATED_IMAGEURL = "http://placehold.it/40x40";

    private static final String DEFAULT_LANGKEY = "en";
    private static final String UPDATED_LANGKEY = "fr";

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MockMvc restUserMockMvc;

    private User user;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    public void beforeEach() {
        cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)
            .clear();
        cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)
            .clear();

        user = initTestUser(userRepository);
    }

    @Test
    @Transactional
    void createUser() throws Exception {
        List<User> allUsers = userRepository.findAll();

        int databaseSizeBeforeCreate = allUsers.size();

        // Create the User
        AdminUserDTO user = new AdminUserDTO();

        user.setLogin(DEFAULT_LOGIN);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setEmail(DEFAULT_EMAIL);
        user.setActivated(true);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        restUserMockMvc
            .perform(
                post("/api/platform/internal/users").contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isCreated());

        // Validate the User in the database
        assertPersistedUsers(users -> {
            assertThat(users).hasSize(databaseSizeBeforeCreate + 1);

            User testUser = users.get(users.size() - 1);

            assertThat(testUser.getLogin()).isEqualTo(DEFAULT_LOGIN);
            assertThat(testUser.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
            assertThat(testUser.getLastName()).isEqualTo(DEFAULT_LASTNAME);
            assertThat(testUser.getEmail()).isEqualTo(DEFAULT_EMAIL);
            assertThat(testUser.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
            assertThat(testUser.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
        });
    }

    @Test
    @Transactional
    void tetsCreateUserWithExistingId() throws Exception {
        List<User> allUsers = userRepository.findAll();

        int databaseSizeBeforeCreate = allUsers.size();

        AdminUserDTO user = new AdminUserDTO();

        user.setId(DEFAULT_ID);
        user.setLogin(DEFAULT_LOGIN);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setEmail(DEFAULT_EMAIL);
        user.setActivated(true);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserMockMvc
            .perform(
                post("/api/platform/internal/users").contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        // Validate the User in the database
        assertPersistedUsers(users -> assertThat(users).hasSize(databaseSizeBeforeCreate));
    }

    @Test
    @Transactional
    void testCreateUserWithExistingLogin() throws Exception {
        // Initialize the database
        userRepository.save(user);

        List<User> allUsers = userRepository.findAll();

        int databaseSizeBeforeCreate = allUsers.size();

        AdminUserDTO user = new AdminUserDTO();

        user.setLogin(DEFAULT_LOGIN); // this login should already be used
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setEmail("anothermail@localhost");
        user.setActivated(true);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        // Create the User
        restUserMockMvc
            .perform(post("/api/platform/internal/users").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(user))
                .with(csrf()))
            .andExpect(status().isBadRequest());

        // Validate the User in the database
        assertPersistedUsers(users -> assertThat(users).hasSize(databaseSizeBeforeCreate));
    }

    @Test
    @Transactional
    void testCreateUserWithExistingEmail() throws Exception {
        // Initialize the database
        userRepository.save(user);

        List<User> allUsers = userRepository.findAll();

        int databaseSizeBeforeCreate = allUsers.size();

        AdminUserDTO user = new AdminUserDTO();
        user.setLogin("anotherlogin");
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setEmail(DEFAULT_EMAIL); // this email should already be used
        user.setActivated(true);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        // Create the User
        restUserMockMvc
            .perform(
                post("/api/platform/internal/users").contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        // Validate the User in the database
        assertPersistedUsers(users -> assertThat(users).hasSize(databaseSizeBeforeCreate));
    }

    @Test
    @Transactional
    void testGetAllUsers() throws Exception {
        // Initialize the database
        userRepository.save(user);

        // Get all the users
        restUserMockMvc
            .perform(get("/api/platform/internal/users?sort=id,desc").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))
            .andExpect(jsonPath("$.[*].firstName").value(hasItem(DEFAULT_FIRSTNAME)))
            .andExpect(jsonPath("$.[*].lastName").value(hasItem(DEFAULT_LASTNAME)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGEURL)))
            .andExpect(jsonPath("$.[*].langKey").value(hasItem(DEFAULT_LANGKEY)));
    }

    @Test
    @Transactional
    void testGetUser() throws Exception {
        // Initialize the database
        userRepository.save(user);

        assertThat(
            cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)
                .get(TenantCacheKeyUtils.getKey(user.getLogin()))).isNull();

        // Get the user
        restUserMockMvc
            .perform(get("/api/platform/internal/users/{login}", user.getLogin()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(user.getId()))
            .andExpect(jsonPath("$.login").value(user.getLogin()))
            .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
            .andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
            .andExpect(jsonPath("$.imageUrl").value(DEFAULT_IMAGEURL))
            .andExpect(jsonPath("$.langKey").value(DEFAULT_LANGKEY));

        assertThat(
            cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)
                .get(TenantCacheKeyUtils.getKey(user.getLogin()))).isNotNull();
    }

    @Test
    @Transactional
    void testGetNonExistingUser() throws Exception {
        restUserMockMvc.perform(get("/api/platform/internal/users/unknown"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void testUpdateUser() throws Exception {
        // Initialize the database
        userRepository.save(user);

        List<User> allUsers = userRepository.findAll();

        int databaseSizeBeforeUpdate = allUsers.size();

        // Update the user
        User updatedUser = userRepository.findById(user.getId())
            .orElseThrow();

        AdminUserDTO user = new AdminUserDTO();

        user.setId(updatedUser.getId());
        user.setLogin(updatedUser.getLogin());
        user.setFirstName(UPDATED_FIRSTNAME);
        user.setLastName(UPDATED_LASTNAME);
        user.setEmail(UPDATED_EMAIL);
        user.setActivated(updatedUser.isActivated());
        user.setImageUrl(UPDATED_IMAGEURL);
        user.setLangKey(UPDATED_LANGKEY);
        user.setCreatedBy(updatedUser.getCreatedBy());
        user.setCreatedDate(updatedUser.getCreatedDate());
        user.setLastModifiedBy(updatedUser.getLastModifiedBy());
        user.setLastModifiedDate(updatedUser.getLastModifiedDate());
        user.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        restUserMockMvc
            .perform(
                put("/api/platform/internal/users").contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isOk());

        // Validate the User in the database
        assertPersistedUsers(users -> {
            assertThat(users).hasSize(databaseSizeBeforeUpdate);
            User testUser = users.stream()
                .filter(usr -> usr.getId()
                    .equals(updatedUser.getId()))
                .findFirst()
                .orElseThrow();
            assertThat(testUser.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
            assertThat(testUser.getLastName()).isEqualTo(UPDATED_LASTNAME);
            assertThat(testUser.getEmail()).isEqualTo(UPDATED_EMAIL);
            assertThat(testUser.getImageUrl()).isEqualTo(UPDATED_IMAGEURL);
            assertThat(testUser.getLangKey()).isEqualTo(UPDATED_LANGKEY);
        });
    }

    @Test
    @Transactional
    void testUpdateUserLogin() throws Exception {
        // Initialize the database
        userRepository.save(user);

        List<User> allUsers = userRepository.findAll();

        int databaseSizeBeforeUpdate = allUsers.size();

        // Update the user
        User updatedUser = userRepository.findById(user.getId())
            .orElseThrow();

        AdminUserDTO user = new AdminUserDTO();

        user.setId(updatedUser.getId());
        user.setLogin(UPDATED_LOGIN);
        user.setFirstName(UPDATED_FIRSTNAME);
        user.setLastName(UPDATED_LASTNAME);
        user.setEmail(UPDATED_EMAIL);
        user.setActivated(updatedUser.isActivated());
        user.setImageUrl(UPDATED_IMAGEURL);
        user.setLangKey(UPDATED_LANGKEY);
        user.setCreatedBy(updatedUser.getCreatedBy());
        user.setCreatedDate(updatedUser.getCreatedDate());
        user.setLastModifiedBy(updatedUser.getLastModifiedBy());
        user.setLastModifiedDate(updatedUser.getLastModifiedDate());
        user.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        restUserMockMvc
            .perform(
                put("/api/platform/internal/users").contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isOk());

        // Validate the User in the database
        assertPersistedUsers(users -> {
            assertThat(users).hasSize(databaseSizeBeforeUpdate);

            User testUser = users.stream()
                .filter(usr -> usr.getId()
                    .equals(updatedUser.getId()))
                .findFirst()
                .orElseThrow();

            assertThat(testUser.getLogin()).isEqualTo(UPDATED_LOGIN);
            assertThat(testUser.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
            assertThat(testUser.getLastName()).isEqualTo(UPDATED_LASTNAME);
            assertThat(testUser.getEmail()).isEqualTo(UPDATED_EMAIL);
            assertThat(testUser.getImageUrl()).isEqualTo(UPDATED_IMAGEURL);
            assertThat(testUser.getLangKey()).isEqualTo(UPDATED_LANGKEY);
        });
    }

    @Test
    @Transactional
    void testUpdateUserExistingEmail() throws Exception {
        // Initialize the database with 2 users
        userRepository.save(user);

        User anotherUser = new User();

        anotherUser.setLogin("bytechef");
        anotherUser.setPassword(RandomStringUtils.randomAlphanumeric(60));
        anotherUser.setActivated(true);
        anotherUser.setEmail("bytechef@localhost");
        anotherUser.setFirstName("java");
        anotherUser.setLastName("hipster");
        anotherUser.setImageUrl("");
        anotherUser.setLangKey("en");

        userRepository.save(anotherUser);

        // Update the user
        User updatedUser = userRepository.findById(user.getId())
            .orElseThrow();

        AdminUserDTO user = new AdminUserDTO();

        user.setId(updatedUser.getId());
        user.setLogin(updatedUser.getLogin());
        user.setFirstName(updatedUser.getFirstName());
        user.setLastName(updatedUser.getLastName());
        user.setEmail("bytechef@localhost"); // this email should already be used by anotherUser
        user.setActivated(updatedUser.isActivated());
        user.setImageUrl(updatedUser.getImageUrl());
        user.setLangKey(updatedUser.getLangKey());
        user.setCreatedBy(updatedUser.getCreatedBy());
        user.setCreatedDate(updatedUser.getCreatedDate());
        user.setLastModifiedBy(updatedUser.getLastModifiedBy());
        user.setLastModifiedDate(updatedUser.getLastModifiedDate());
        user.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        restUserMockMvc
            .perform(
                put("/api/platform/internal/users").contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testUpdateUserExistingLogin() throws Exception {
        // Initialize the database
        userRepository.save(user);

        User anotherUser = new User();

        anotherUser.setLogin("bytechef");
        anotherUser.setPassword(RandomStringUtils.randomAlphanumeric(60));
        anotherUser.setActivated(true);
        anotherUser.setEmail("bytechef@localhost");
        anotherUser.setFirstName("firstName");
        anotherUser.setLastName("lastName");
        anotherUser.setImageUrl("");
        anotherUser.setLangKey("en");

        userRepository.save(anotherUser);

        // Update the user
        User updatedUser = userRepository.findById(user.getId())
            .orElseThrow();

        AdminUserDTO user = new AdminUserDTO();

        user.setId(updatedUser.getId());
        user.setLogin("bytechef"); // this login should already be used by anotherUser
        user.setFirstName(updatedUser.getFirstName());
        user.setLastName(updatedUser.getLastName());
        user.setEmail(updatedUser.getEmail());
        user.setActivated(updatedUser.isActivated());
        user.setImageUrl(updatedUser.getImageUrl());
        user.setLangKey(updatedUser.getLangKey());
        user.setCreatedBy(updatedUser.getCreatedBy());
        user.setCreatedDate(updatedUser.getCreatedDate());
        user.setLastModifiedBy(updatedUser.getLastModifiedBy());
        user.setLastModifiedDate(updatedUser.getLastModifiedDate());
        user.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        restUserMockMvc
            .perform(
                put("/api/platform/internal/users").contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testDeleteUser() throws Exception {
        // Initialize the database
        userRepository.save(user);

        List<User> allUsers = userRepository.findAll();

        int databaseSizeBeforeDelete = allUsers.size();

        // Delete the user
        restUserMockMvc
            .perform(
                delete("/api/platform/internal/users/{login}", user.getLogin()).accept(MediaType.APPLICATION_JSON)
                    .with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(
            cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)
                .get(TenantCacheKeyUtils.getKey(user.getLogin()))).isNull();

        // Validate the database is empty
        assertPersistedUsers(users -> assertThat(users).hasSize(databaseSizeBeforeDelete - 1));
    }

    @Test
    void testUserEquals() throws Exception {
//        TestUtil.equalsVerifier(User.class);

        User user1 = new User();

        user1.setId(DEFAULT_ID);
        User user2 = new User();
        user2.setId(user1.getId());

        assertThat(user1).isEqualTo(user2);

        user2.setId(2L);

        assertThat(user1).isNotEqualTo(user2);

        user1.setId(null);

        assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    void testUserDTOtoUser() {
        AdminUserDTO userDTO = new AdminUserDTO();

        userDTO.setId(DEFAULT_ID);
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setFirstName(DEFAULT_FIRSTNAME);
        userDTO.setLastName(DEFAULT_LASTNAME);
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setActivated(true);
        userDTO.setImageUrl(DEFAULT_IMAGEURL);
        userDTO.setLangKey(DEFAULT_LANGKEY);
        userDTO.setCreatedBy(DEFAULT_LOGIN);
        userDTO.setLastModifiedBy(DEFAULT_LOGIN);
        userDTO.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        User user = userMapper.userDTOToUser(userDTO);

        assertThat(user.getId()).isEqualTo(DEFAULT_ID);
        assertThat(user.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(user.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(user.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(user.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(user.isActivated()).isTrue();
        assertThat(user.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        assertThat(user.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
        assertThat(user.getCreatedBy()).isNull();
        assertThat(user.getCreatedDate()).isNull();
        assertThat(user.getLastModifiedBy()).isNull();
        assertThat(user.getLastModifiedDate()).isNull();
//        assertThat(user.getAuthorities()).extracting("name")
//            .containsExactly(AuthoritiesConstants.USER);
    }

    @Test
    void testUserToUserDTO() {
        user.setId(DEFAULT_ID);
//        user.setCreatedBy(DEFAULT_LOGIN);
//        user.setCreatedDate(Instant.now());
//        user.setLastModifiedBy(DEFAULT_LOGIN);
//        user.setLastModifiedDate(Instant.now());

        Set<Authority> authorities = new HashSet<>();

        Authority authority = new Authority();

        authority.setId(1L);
        authority.setName(AuthorityConstants.USER);

        authorities.add(authority);

        user.setAuthorities(authorities);

        AdminUserDTO userDTO = userMapper.userToAdminUserDTO(user, List.of(authority));

        assertThat(userDTO.getId()).isEqualTo(DEFAULT_ID);
        assertThat(userDTO.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userDTO.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(userDTO.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(userDTO.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(userDTO.isActivated()).isTrue();
        assertThat(userDTO.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        assertThat(userDTO.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
//        assertThat(userDTO.getCreatedBy()).isEqualTo(DEFAULT_LOGIN);
//        assertThat(userDTO.getCreatedDate()).isEqualTo(user.getCreatedDate());
//        assertThat(userDTO.getLastModifiedBy()).isEqualTo(DEFAULT_LOGIN);
//        assertThat(userDTO.getLastModifiedDate()).isEqualTo(user.getLastModifiedDate());
        assertThat(userDTO.getAuthorities()).containsExactly(AuthorityConstants.USER);
        assertThat(userDTO.toString()).isNotNull();
    }

    private void assertPersistedUsers(Consumer<List<User>> userAssertion) {
        userAssertion.accept(userRepository.findAll());
    }

    private static User createEntity() {
        User user = new User();

        user.setLogin(DEFAULT_LOGIN + RandomStringUtils.randomAlphabetic(5));
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(true);
        user.setEmail(RandomStringUtils.randomAlphabetic(5) + DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);

        return user;
    }

    private static User initTestUser(UserRepository userRepository) {
        userRepository.deleteAll();

        User user = createEntity();

        user.setLogin(DEFAULT_LOGIN);
        user.setEmail(DEFAULT_EMAIL);

        return user;
    }
}
