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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.dto.PasswordChangeDTO;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.web.rest.config.UserIntTestConfiguration;
import com.bytechef.platform.user.web.rest.config.UserIntTestConfigurationSharedMocks;
import com.bytechef.platform.user.web.rest.vm.KeyAndPasswordVM;
import com.bytechef.platform.user.web.rest.vm.ManagedUserVM;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link AccountController} REST controller.
 *
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = UserIntTestConfiguration.class, properties = "bytechef.tenant.mode=single")
@UserIntTestConfigurationSharedMocks
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class AccountControllerIntTest {

    static final String TEST_USER_LOGIN = "test";

    @Autowired
    private AuthorityRepository authorityRepository;

    @MockitoBean
    private MailService mailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PersistentTokenRepository persistentTokenRepository;

    @Autowired
    private MockMvc restAccountMockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    @WithUnauthenticatedMockUser
    void testNonAuthenticatedUser() throws Exception {
        restAccountMockMvc
            .perform(get("/api/authenticate").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    @Test
    @WithMockUser(TEST_USER_LOGIN)
    void testAuthenticatedUser() throws Exception {
        restAccountMockMvc
            .perform(
                get("/api/authenticate")
                    .with(request -> request)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(TEST_USER_LOGIN));
    }

    @Test
    @WithMockUser(TEST_USER_LOGIN)
    void testGetExistingAccount() throws Exception {
        Set<String> authorities = new HashSet<>();

        authorities.add(AuthorityConstants.ADMIN);

        AdminUserDTO user = new AdminUserDTO();

        user.setLogin(TEST_USER_LOGIN);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setEmail("john.doe@jhipster.com");
        user.setImageUrl("http://placehold.it/50x50");
        user.setLangKey("en");
        user.setAuthorities(authorities);

        userService.create(user);

        restAccountMockMvc
            .perform(get("/api/account").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.login").value(TEST_USER_LOGIN))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.email").value("john.doe@jhipster.com"))
            .andExpect(jsonPath("$.imageUrl").value("http://placehold.it/50x50"))
            .andExpect(jsonPath("$.langKey").value("en"))
            .andExpect(jsonPath("$.authorities").value(AuthorityConstants.ADMIN));
    }

    @Test
    void testGetUnknownAccount() throws Exception {
        restAccountMockMvc.perform(get("/api/account").accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
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
    @Transactional
    void testRegisterInvalidLogin() throws Exception {
        ManagedUserVM invalidUser = new ManagedUserVM();

        invalidUser.setLogin("funky-log(n"); // <-- invalid
        invalidUser.setPassword("Password1");
        invalidUser.setFirstName("Funky");
        invalidUser.setLastName("One");
        invalidUser.setEmail("funky@example.com");
        invalidUser.setActivated(true);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(invalidUser))
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findByEmailIgnoreCase("funky@example.com");

        assertThat(user).isEmpty();
    }

    static Stream<ManagedUserVM> invalidUsers() {
        return Stream.of(
            createInvalidUser("bob", "password", "Bob", "Green", "invalid", true), // <-- invalid
            createInvalidUser("bob", "123", "Bob", "Green", "bob@example.com", true), // password with only 3 digits
            createInvalidUser("bob", null, "Bob", "Green", "bob@example.com", true) // invalid null password
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUsers")
    @Transactional
    void testRegisterInvalidUsers(ManagedUserVM invalidUser) throws Exception {
        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(invalidUser))
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findByLogin("bob");

        assertThat(user).isEmpty();
    }

    private static ManagedUserVM createInvalidUser(
        String login, String password, String firstName, String lastName, String email, boolean activated) {

        ManagedUserVM invalidUser = new ManagedUserVM();

        invalidUser.setLogin(login);
        invalidUser.setPassword(password);
        invalidUser.setFirstName(firstName);
        invalidUser.setLastName(lastName);
        invalidUser.setEmail(email);
        invalidUser.setActivated(activated);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        return invalidUser;
    }

    @Test
    @Transactional
    void testRegisterDuplicateLogin() throws Exception {
        // First registration
        ManagedUserVM firstUser = new ManagedUserVM();

        firstUser.setLogin("alice");
        firstUser.setPassword("Password1");
        firstUser.setFirstName("Alice");
        firstUser.setLastName("Something");
        firstUser.setEmail("alice@example.com");
        firstUser.setImageUrl("http://placehold.it/50x50");
        firstUser.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        firstUser.setAuthorities(Collections.singleton(AuthorityConstants.USER));

        // Duplicate login, different email
        ManagedUserVM secondUser = new ManagedUserVM();

        secondUser.setLogin(firstUser.getLogin());
        secondUser.setPassword(firstUser.getPassword());
        secondUser.setFirstName(firstUser.getFirstName());
        secondUser.setLastName(firstUser.getLastName());
        secondUser.setEmail("alice2@example.com");
        secondUser.setImageUrl(firstUser.getImageUrl());
        secondUser.setLangKey(firstUser.getLangKey());
        secondUser.setCreatedBy(firstUser.getCreatedBy());
        secondUser.setCreatedDate(firstUser.getCreatedDate());
        secondUser.setLastModifiedBy(firstUser.getLastModifiedBy());
        secondUser.setLastModifiedDate(firstUser.getLastModifiedDate());
        secondUser.setAuthorities(new HashSet<>(firstUser.getAuthorities()));

        // First user
        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(firstUser))
                    .with(csrf()))
            .andExpect(status().isCreated());

        // Second (non activated) user
        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(secondUser))
                    .with(csrf()))
            .andExpect(status().isCreated());

        Optional<User> testUser = userRepository.findByEmailIgnoreCase("alice2@example.com");

        assertThat(testUser).isPresent();

        testUser.orElseThrow()
            .setActivated(true);

        userRepository.save(testUser.orElseThrow());

        // Second (already activated) user
        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(secondUser))
                    .with(csrf()))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @Transactional
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

        Optional<User> testUser1 = userRepository.findByLogin("test-register-duplicate-email");

        assertThat(testUser1).isPresent();

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

        Optional<User> testUser2 = userRepository.findByLogin("test-register-duplicate-email");

        assertThat(testUser2).isEmpty();

        Optional<User> testUser3 = userRepository.findByLogin("test-register-duplicate-email-2");

        assertThat(testUser3).isPresent();

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

        Optional<User> testUser4 = userRepository.findByLogin("test-register-duplicate-email-3");

        assertThat(testUser4).isPresent();
        assertThat(testUser4.orElseThrow()
            .getEmail()).isEqualTo("test-register-duplicate-email@example.com");

        testUser4.orElseThrow()
            .setActivated(true);

        userService.update(
            new AdminUserDTO(testUser4.orElseThrow(), List.of(new Authority().id(1L)
                .name(AuthorityConstants.USER))));

        // Register 4th (already activated) user
        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(secondUser))
                    .with(csrf()))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @Transactional
    void testRegisterDuplicateAdmin() throws Exception {
        User existingUser = new User();

        existingUser.setLogin("test-register-valid");
        existingUser.setPassword("Password1");
        existingUser.setFirstName("Alice");
        existingUser.setLastName("Test");
        existingUser.setEmail("test-register-valid@example.com");
        existingUser.setActivated(true);
        existingUser.setImageUrl("http://placehold.it/50x50");
        existingUser.setLangKey(UserConstants.DEFAULT_LANGUAGE);

        assertThat(userRepository.findByLogin("test-register-valid")).isEmpty();

        userRepository.save(existingUser);

        ManagedUserVM validUser = new ManagedUserVM();

        validUser.setLogin("badguy");
        validUser.setPassword("Password1");
        validUser.setFirstName("Bad");
        validUser.setLastName("Guy");
        validUser.setEmail("badguy@example.com");
        validUser.setActivated(true);
        validUser.setImageUrl("http://placehold.it/50x50");
        validUser.setLangKey(UserConstants.DEFAULT_LANGUAGE);

        restAccountMockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(validUser))
                    .with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testActivateAccount() throws Exception {
        final String activationKey = "some activation key";

        User user = new User();

        user.setLogin("activate-account");
        user.setEmail("activate-account@example.com");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(false);
        user.setActivationKey(activationKey);

        userRepository.save(user);

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", activationKey))
            .andExpect(status().isOk());

        user = userRepository.findByLogin(user.getLogin())
            .orElse(null);

        assertThat(user.isActivated()).isTrue();
    }

    @Test
    @Transactional
    void testActivateAccountWithWrongKey() throws Exception {
        restAccountMockMvc.perform(get("/api/activate?key=wrongActivationKey"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @Transactional
    @WithMockUser("save-account")
    void testSaveAccount() throws Exception {
        User user = new User();

        user.setLogin("save-account");
        user.setEmail("save-account@example.com");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(true);

        userRepository.save(user);

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
                    .with(csrf()))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findByLogin(user.getLogin())
            .orElse(null);

        assertThat(updatedUser.getFirstName()).isEqualTo(userDTO.getFirstName());
        assertThat(updatedUser.getLastName()).isEqualTo(userDTO.getLastName());
        assertThat(updatedUser.getEmail()).isEqualTo(userDTO.getEmail());
        assertThat(updatedUser.getLangKey()).isEqualTo(userDTO.getLangKey());
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(updatedUser.getImageUrl()).isEqualTo(userDTO.getImageUrl());
        assertThat(updatedUser.isActivated()).isTrue();
        assertThat(updatedUser.getAuthorityIds()).isEmpty();
    }

    @Test
    @Transactional
    @WithMockUser("save-invalid-email")
    void testSaveInvalidEmail() throws Exception {
        User user = new User();

        user.setLogin("save-invalid-email");
        user.setEmail("save-invalid-email@example.com");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(true);

        userRepository.save(user);

        AdminUserDTO userDTO = new AdminUserDTO();

        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("invalid email");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthorityConstants.ADMIN));

        restAccountMockMvc
            .perform(
                post("/api/account").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(userDTO))
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmailIgnoreCase("invalid email")).isNotPresent();
    }

    @Test
    @Transactional
    @WithMockUser("save-existing-email")
    void testSaveExistingEmail() throws Exception {
        User user = new User();

        user.setLogin("save-existing-email");
        user.setEmail("save-existing-email@example.com");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(true);

        userRepository.save(user);

        User anotherUser = new User();

        anotherUser.setLogin("save-existing-email2");
        anotherUser.setEmail("save-existing-email2@example.com");
        anotherUser.setPassword(RandomStringUtils.randomAlphanumeric(60));
        anotherUser.setActivated(true);

        userRepository.save(anotherUser);

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
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findByLogin("save-existing-email")
            .orElse(null);

        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("save-existing-email-and-login")
    void testSaveExistingEmailAndLogin() throws Exception {
        User user = new User();

        user.setLogin("save-existing-email-and-login");
        user.setEmail("save-existing-email-and-login@example.com");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(true);

        userRepository.save(user);

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
                    .with(csrf()))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findByLogin("save-existing-email-and-login")
            .orElse(null);

        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email-and-login@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("change-password-wrong-existing-password")
    void testChangePasswordWrongExistingPassword() throws Exception {
        User user = new User();

        String currentPassword = RandomStringUtils.randomAlphanumeric(60);

        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-wrong-existing-password");
        user.setEmail("change-password-wrong-existing-password@example.com");

        userRepository.save(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(new PasswordChangeDTO("1" + currentPassword, "NewPassword1")))
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findByLogin("change-password-wrong-existing-password")
            .orElse(null);

        assertThat(passwordEncoder.matches("NewPassword1", updatedUser.getPassword())).isFalse();
        assertThat(passwordEncoder.matches(currentPassword, updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    @WithMockUser("change-password")
    void testChangePassword() throws Exception {
        User user = new User();

        String currentPassword = RandomStringUtils.randomAlphanumeric(60);

        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password");
        user.setEmail("change-password@example.com");

        userRepository.save(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(new PasswordChangeDTO(currentPassword, "NewPassword1")))
                    .with(csrf()))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findByLogin("change-password")
            .orElse(null);

        assertThat(passwordEncoder.matches("NewPassword1", updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    @WithMockUser("change-password-too-small")
    void testChangePasswordTooSmall() throws Exception {
        User user = new User();

        String currentPassword = RandomStringUtils.randomAlphanumeric(60);

        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-too-small");
        user.setEmail("change-password-too-small@example.com");

        userRepository.save(user);

        String newPassword = RandomStringUtils.random(ManagedUserVM.PASSWORD_MIN_LENGTH - 1);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(new PasswordChangeDTO(currentPassword, newPassword)))
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findByLogin("change-password-too-small")
            .orElse(null);

        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional
    @WithMockUser("change-password-too-long")
    void testChangePasswordTooLong() throws Exception {
        User user = new User();

        String currentPassword = RandomStringUtils.randomAlphanumeric(60);

        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-too-long");
        user.setEmail("change-password-too-long@example.com");

        userRepository.save(user);

        String newPassword = RandomStringUtils.random(ManagedUserVM.PASSWORD_MAX_LENGTH + 1);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(new PasswordChangeDTO(currentPassword, newPassword)))
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findByLogin("change-password-too-long")
            .orElse(null);

        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional
    @WithMockUser("change-password-empty")
    void testChangePasswordEmpty() throws Exception {
        User user = new User();

        String currentPassword = RandomStringUtils.randomAlphanumeric(60);

        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-empty");
        user.setEmail("change-password-empty@example.com");

        userRepository.save(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(new PasswordChangeDTO(currentPassword, "")))
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findByLogin("change-password-empty")
            .orElse(null);

        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional
    @WithMockUser("current-sessions")
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    void testGetCurrentSessions() throws Exception {
        User user = new User();

        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setLogin("current-sessions");
        user.setEmail("current-sessions@example.com");

        userRepository.save(user);

        PersistentToken token = new PersistentToken();

        token.setNew(true);
        token.setSeries("current-sessions");
        token.setUser(user);
        token.setTokenValue("current-session-data");
        token.setTokenDate(LocalDate.of(2017, 3, 23));

        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Test agent");

        persistentTokenRepository.save(token);

        restAccountMockMvc
            .perform(get("/api/account/sessions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[*].series").value(hasItem(token.getSeries())))
            .andExpect(jsonPath("$.[*].ipAddress").value(hasItem(token.getIpAddress())))
            .andExpect(jsonPath("$.[*].userAgent").value(hasItem(token.getUserAgent())))
            .andExpect(jsonPath("$.[*].tokenDate").value(hasItem(containsString(token.getTokenDate()
                .toString()))));
    }

    @Test
    @Transactional
    @WithMockUser("invalidate-session")
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    void testInvalidateSession() throws Exception {
        User user = new User();

        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setLogin("invalidate-session");
        user.setEmail("invalidate-session@example.com");

        userRepository.save(user);

        PersistentToken token = new PersistentToken();

        token.setNew(true);
        token.setSeries("invalidate-session");
        token.setUser(user);
        token.setTokenValue("invalidate-data");
        token.setTokenDate(LocalDate.of(2017, 3, 23));
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Test agent");

        persistentTokenRepository.save(token);

        assertThat(persistentTokenRepository.findAllByUserId(user.getId())).hasSize(1);

        restAccountMockMvc.perform(delete("/api/account/sessions/invalidate-session").with(csrf()))
            .andExpect(status().isOk());

        assertThat(persistentTokenRepository.findAllByUserId(user.getId())).isEmpty();
    }

    @Test
    @Transactional
    void testRequestPasswordReset() throws Exception {
        User user = new User();

        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(true);
        user.setLogin("password-reset");
        user.setEmail("password-reset@example.com");
        user.setLangKey("en");

        userRepository.save(user);

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/init").content("password-reset@example.com")
                    .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void testRequestPasswordResetUpperCaseEmail() throws Exception {
        User user = new User();

        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(true);
        user.setLogin("password-reset-upper-case");
        user.setEmail("password-reset-upper-case@example.com");
        user.setLangKey("en");

        userRepository.save(user);

        restAccountMockMvc
            .perform(post("/api/account/reset-password/init").content("password-reset-upper-case@EXAMPLE.COM")
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    void testRequestPasswordResetWrongEmail() throws Exception {
        restAccountMockMvc
            .perform(post("/api/account/reset-password/init").content("password-reset-wrong-email@example.com")
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void testFinishPasswordReset() throws Exception {
        User user = new User();

        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setLogin("finish-password-reset");
        user.setEmail("finish-password-reset@example.com");
        user.setResetDate(Instant.now()
            .plusSeconds(60));
        user.setResetKey("reset key");

        userRepository.save(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();

        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("NewPassword1");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(keyAndPassword))
                    .with(csrf()))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findByLogin(user.getLogin())
            .orElse(null);

        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    void testFinishPasswordResetTooSmall() throws Exception {
        User user = new User();

        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setLogin("finish-password-reset-too-small");
        user.setEmail("finish-password-reset-too-small@example.com");
        user.setResetDate(Instant.now()
            .plusSeconds(60));
        user.setResetKey("reset key too small");

        userRepository.save(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("foo");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(keyAndPassword))
                    .with(csrf()))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findByLogin(user.getLogin())
            .orElse(null);

        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isFalse();
    }

    @Test
    @Transactional
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
}
