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

import static com.bytechef.platform.user.web.rest.MultiTenantAccountControllerIntTest.MultiTenantAccountControllerConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.tenant.multi.sql.MultiTenantDataSource;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.web.rest.config.UserIntTestConfiguration;
import com.bytechef.platform.user.web.rest.config.UserIntTestConfigurationSharedMocks;
import com.bytechef.platform.user.web.rest.vm.KeyAndPasswordVM;
import com.bytechef.platform.user.web.rest.vm.ManagedUserVM;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link AccountController} REST controller.
 *
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc
@SpringBootTest(
    classes = {
        UserIntTestConfiguration.class, MultiTenantAccountControllerConfiguration.class
    }, properties = {
        "bytechef.tenant.mode=multi", "bytechef.edition=EE", "spring.main.allow-bean-definition-overriding=true"
    })
@UserIntTestConfigurationSharedMocks
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class MultiTenantAccountControllerIntTest {

    @MockitoBean
    private MailService mailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc restAccountMockMvc;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @AfterAll
    public static void afterAll() {
        TenantContext.resetCurrentTenantId();
    }

    @BeforeEach
    public void beforeEach() {
        List<String> tenantIds = tenantService.getTenantIds();

        for (String tenantId : tenantIds) {
            tenantService.deleteTenant(tenantId);
        }

        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testRegisterDuplicateEmail() throws Exception {
        // First user
        ManagedUserVM firstUser = new ManagedUserVM();

        firstUser.setLogin("test-register-duplicate-email");
        firstUser.setPassword("Password1");
        firstUser.setFirstName("Alice");
        firstUser.setLastName("Test");
        firstUser.setEmail("test-register-duplicate-email@example.com");
        firstUser.setImageUrl("http://placehold.it/50x50");
        firstUser.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        firstUser.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        // Register first user
        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(firstUser))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUser1Optional = userRepository.findByLogin("test-register-duplicate-email");

        assertThat(testUser1Optional).isPresent();

        // Duplicate email, different login
        ManagedUserVM secondUser = new ManagedUserVM();

        secondUser.setLogin("test-register-duplicate-email-2");
        secondUser.setPassword(firstUser.getPassword());
        secondUser.setFirstName(firstUser.getFirstName());
        secondUser.setLastName(firstUser.getLastName());
        secondUser.setEmail(firstUser.getEmail());
        secondUser.setImageUrl(firstUser.getImageUrl());
        secondUser.setLangKey(firstUser.getLangKey());
        secondUser.setAuthorities(new HashSet<>(firstUser.getAuthorities()));

        // Register second (non activated) user
        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(secondUser))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUser2Optional = userRepository.findByLogin("test-register-duplicate-email");

        assertThat(testUser2Optional).isEmpty();

        Optional<User> testUser3Optional = userRepository.findByLogin("test-register-duplicate-email-2");

        assertThat(testUser3Optional).isPresent();

        // Duplicate email - with uppercase email address
        ManagedUserVM userWithUpperCaseEmail = new ManagedUserVM();

        userWithUpperCaseEmail.setId(firstUser.getId());
        userWithUpperCaseEmail.setLogin("test-register-duplicate-email-3");
        userWithUpperCaseEmail.setPassword(firstUser.getPassword());
        userWithUpperCaseEmail.setFirstName(firstUser.getFirstName());
        userWithUpperCaseEmail.setLastName(firstUser.getLastName());
        userWithUpperCaseEmail.setEmail("TEST-register-duplicate-email@example.com");
        userWithUpperCaseEmail.setImageUrl(firstUser.getImageUrl());
        userWithUpperCaseEmail.setLangKey(firstUser.getLangKey());
        userWithUpperCaseEmail.setAuthorities(new HashSet<>(firstUser.getAuthorities()));

        // Register third (not activated) user
        restAccountMockMvc
            .perform(
                post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(userWithUpperCaseEmail))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUser4Optional = userRepository.findByLogin("test-register-duplicate-email-3");

        assertThat(testUser4Optional).isPresent();

        User testUser4 = testUser4Optional.orElseThrow();

        assertThat(testUser4.getEmail()).isEqualTo("test-register-duplicate-email@example.com");

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", testUser4.getActivationKey()))
            .andExpect(status().isOk());

        // Register 4th (already activated) user
        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(secondUser))
                    .with(csrf()))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void testRegisterValid() throws Exception {
        ManagedUserVM validUser = new ManagedUserVM();

        validUser.setLogin("test-register-valid");
        validUser.setPassword("Password1");
        validUser.setFirstName("Alice");
        validUser.setLastName("Test");
        validUser.setEmail("test-register-valid@example.com");
        validUser.setImageUrl("http://placehold.it/50x50");
        validUser.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        validUser.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        assertThat(userRepository.findByLogin("test-register-valid")).isEmpty();

        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(validUser))
                    .with(csrf()))
            .andExpect(status().isCreated());

        assertThat(userRepository.findByLogin("test-register-valid")).isPresent();
    }

    @Test
    void testFinishPasswordReset() throws Exception {
        User user = new User();

        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setLogin("finish-password-reset");
        user.setEmail("finish-password-reset@example.com");

        restAccountMockMvc
            .perform(
                post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUserOptional = userRepository.findByLogin("finish-password-reset");

        assertThat(testUserOptional).isPresent();

        User testUser = testUserOptional.orElseThrow();

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", testUser.getActivationKey()))
            .andExpect(status().isOk());

        String resetKey = TenantContext.callWithTenantId("000001", () -> {
            User updatedUser = userRepository.findByLogin(user.getLogin())
                .orElseThrow();

            Instant now = Instant.now();

            updatedUser.setResetDate(now.plusSeconds(60));
            updatedUser.setResetKey("reset key");

            userService.save(updatedUser);

            return updatedUser.getResetKey();
        });

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();

        keyAndPassword.setKey(resetKey);
        keyAndPassword.setNewPassword("NewPassword1");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(keyAndPassword))
                    .with(csrf()))
            .andExpect(status().isOk());

        TenantContext.runWithTenantId("000001", () -> {
            User updatedUser = userRepository.findByLogin(user.getLogin())
                .orElse(null);

            assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isTrue();
        });
    }

    @Test
    void testFinishPasswordResetWrongKey() throws Exception {
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();

        keyAndPassword.setKey("wrong reset key");
        keyAndPassword.setNewPassword("NewPassword1");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(keyAndPassword))
                    .with(csrf()))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testActivateAccount() throws Exception {
        final String activationKey = "some activation key";

        final User user = new User();

        user.setLogin("activate-account");
        user.setEmail("activate-account@example.com");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(false);
        user.setActivationKey(activationKey);

        userRepository.save(user);

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", activationKey))
            .andExpect(status().isOk());

        TenantContext.runWithTenantId("000001", () -> {
            User updatedUser = userRepository.findByLogin(user.getLogin())
                .orElse(null);

            assertThat(updatedUser.isActivated()).isTrue();
        });
    }

    @Test
    void testActivateAccountWithWrongKey() throws Exception {
        restAccountMockMvc.perform(get("/api/activate?key=wrongActivationKey"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testRequestPasswordReset() throws Exception {
        User user = new User();

        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setLogin("password-reset");
        user.setEmail("password-reset@example.com");
        user.setLangKey("en");

        restAccountMockMvc
            .perform(
                post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUserOptional = userRepository.findByLogin("password-reset");

        assertThat(testUserOptional).isPresent();

        User testUser = testUserOptional.orElseThrow();

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", testUser.getActivationKey()))
            .andExpect(status().isOk());

        TenantContext.runWithTenantId("000001", () -> {
            User updatedUser = userRepository.findByLogin(user.getLogin())
                .orElse(null);

            assertThat(updatedUser.getResetKey()).isNull();
        });

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/init").content("password-reset@example.com")
                    .with(csrf()))
            .andExpect(status().isOk());

        TenantContext.runWithTenantId("000001", () -> {
            User updatedUser = userRepository.findByLogin(user.getLogin())
                .orElse(null);

            assertThat(updatedUser.getResetKey()).isNotNull();
        });
    }

    @Test
    @WithMockUser("save-account")
    void testSaveAccount() throws Exception {
        User user = new User();

        user.setLogin("save-account");
        user.setEmail("save-account@example.com");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));

        restAccountMockMvc
            .perform(
                post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUserOptional = userRepository.findByLogin("save-account");

        assertThat(testUserOptional).isPresent();

        User testUser = testUserOptional.orElseThrow();

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", testUser.getActivationKey()))
            .andExpect(status().isOk());

        AdminUserDTO userDTO = new AdminUserDTO();

        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-account@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthorityConstants.ADMIN));

        restAccountMockMvc
            .perform(
                post("/api/account").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(userDTO))
                    .with(csrf())
                    .sessionAttr(TenantConstants.CURRENT_TENANT_ID, "000001"))
            .andExpect(status().isOk());

        TenantContext.runWithTenantId("000001", () -> {
            User updatedUser = userRepository.findByLogin(user.getLogin())
                .orElse(null);

            assertThat(updatedUser.getFirstName()).isEqualTo(userDTO.getFirstName());
            assertThat(updatedUser.getLastName()).isEqualTo(userDTO.getLastName());
            assertThat(updatedUser.getEmail()).isEqualTo(userDTO.getEmail());
            assertThat(updatedUser.getLangKey()).isEqualTo(userDTO.getLangKey());
//            assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
            assertThat(updatedUser.getImageUrl()).isEqualTo(userDTO.getImageUrl());
            assertThat(updatedUser.isActivated()).isTrue();
            assertThat(updatedUser.getAuthorityIds()).hasSize(1);
        });
    }

    @Test
    @WithMockUser("save-existing-email")
    void testSaveExistingEmail() throws Exception {
        User user = new User();

        user.setLogin("save-existing-email");
        user.setEmail("save-existing-email@example.com");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));

        restAccountMockMvc
            .perform(
                post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUserOptional = userRepository.findByLogin("save-existing-email");

        assertThat(testUserOptional).isPresent();

        User testUser = testUserOptional.orElseThrow();

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", testUser.getActivationKey()))
            .andExpect(status().isOk());

        User anotherUser = new User();

        anotherUser.setLogin("save-existing-email2");
        anotherUser.setEmail("save-existing-email2@example.com");
        anotherUser.setPassword(RandomStringUtils.randomAlphanumeric(60));

        restAccountMockMvc
            .perform(
                post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(anotherUser))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUser2Optional = userRepository.findByLogin("save-existing-email2");

        assertThat(testUser2Optional).isPresent();

        User testUser2 = testUser2Optional.orElseThrow();

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", testUser2.getActivationKey()))
            .andExpect(status().isOk());

        AdminUserDTO userDTO = new AdminUserDTO();

        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email2@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthorityConstants.ADMIN));

        restAccountMockMvc
            .perform(
                post("/api/account").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(userDTO))
                    .with(csrf())
                    .sessionAttr(TenantConstants.CURRENT_TENANT_ID, "000001"))
            .andExpect(status().isBadRequest());

        TenantContext.runWithTenantId("000001", () -> {
            User updatedUser = userRepository.findByLogin("save-existing-email")
                .orElse(null);

            assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email@example.com");
        });
    }

    @Test
    @WithMockUser("save-existing-email-and-login")
    void testSaveExistingEmailAndLogin() throws Exception {
        User user = new User();

        user.setLogin("save-existing-email-and-login");
        user.setEmail("save-existing-email-and-login@example.com");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));

        restAccountMockMvc
            .perform(
                post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUserOptional = userRepository.findByLogin("save-existing-email-and-login");

        assertThat(testUserOptional).isPresent();

        User testUser = testUserOptional.orElseThrow();

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", testUser.getActivationKey()))
            .andExpect(status().isOk());

        AdminUserDTO userDTO = new AdminUserDTO();

        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email-and-login@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthorityConstants.ADMIN));

        restAccountMockMvc
            .perform(
                post("/api/account").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(userDTO))
                    .with(csrf())
                    .sessionAttr(TenantConstants.CURRENT_TENANT_ID, "000001"))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findByLogin("save-existing-email-and-login")
            .orElse(null);

        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email-and-login@example.com");
    }

    @Test
    @WithMockUser("save-existing-email")
    void testSaveExistingEmailAnotherTenant() throws Exception {
        User user = new User();

        user.setLogin("save-existing-email-1");
        user.setEmail("save-existing-email-1@example.com");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));

        restAccountMockMvc
            .perform(
                post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(user))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUserOptional = userRepository.findByLogin("save-existing-email-1");

        assertThat(testUserOptional).isPresent();

        User testUser = testUserOptional.orElseThrow();

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", testUser.getActivationKey()))
            .andExpect(status().isOk());

        User anotherUser = new User();

        anotherUser.setLogin("save-existing-email-2");
        anotherUser.setEmail("save-existing-email-2@example.com");
        anotherUser.setPassword(RandomStringUtils.randomAlphanumeric(60));

        restAccountMockMvc
            .perform(
                post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(anotherUser))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUser2Optional = userRepository.findByLogin("save-existing-email-2");

        assertThat(testUser2Optional).isPresent();

        User testUser2 = testUser2Optional.orElseThrow();

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", testUser2.getActivationKey()))
            .andExpect(status().isOk());

        AdminUserDTO userDTO = new AdminUserDTO();

        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email-1@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthorityConstants.ADMIN));

        restAccountMockMvc
            .perform(
                post("/api/account").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(userDTO))
                    .with(csrf())
                    .sessionAttr(TenantConstants.CURRENT_TENANT_ID, "000002"))
            .andExpect(status().isBadRequest());

        TenantContext.runWithTenantId("000001", () -> {
            User updatedUser = userRepository.findByLogin("save-existing-email-1")
                .orElse(null);

            assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email-1@example.com");
        });
    }

    @TestConfiguration
    static class MultiTenantAccountControllerConfiguration {

        @Bean
        DataSource dataSource(PostgreSQLContainer<?> postgreSQLContainer) {
            final HikariDataSource dataSource = new HikariDataSource();

            dataSource.setJdbcUrl(postgreSQLContainer.getJdbcUrl());
            dataSource.setDriverClassName(postgreSQLContainer.getDriverClassName());
            dataSource.setUsername(postgreSQLContainer.getUsername());
            dataSource.setPassword(postgreSQLContainer.getPassword());

            return new MultiTenantDataSource(dataSource);
        }
    }
}
