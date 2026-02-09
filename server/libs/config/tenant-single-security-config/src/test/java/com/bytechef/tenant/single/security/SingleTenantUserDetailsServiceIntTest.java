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

package com.bytechef.tenant.single.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.jdbc.config.AuditingJdbcConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.config.SecurityConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Locale;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.encryption", "com.bytechef.platform.user", "com.bytechef.security",
        "com.bytechef.tenant.single.security"
    })
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableCaching
@Transactional
@SpringBootTest(classes = {
    AuditingJdbcConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class,
    SecurityConfiguration.class
})
class SingleTenantUserDetailsServiceIntTest {

    @TestConfiguration
    static class TestConfig extends AbstractJdbcConfiguration {

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }
    }

    private static final String USER_ONE_LOGIN = "test-user-one";
    private static final String USER_ONE_EMAIL = "test-user-one@localhost.com";
    private static final String USER_TWO_LOGIN = "test-user-two";
    private static final String USER_TWO_EMAIL = "test-user-two@localhost.com";
    private static final String USER_THREE_LOGIN = "test-user-three";
    private static final String USER_THREE_EMAIL = "test-user-three@localhost.com";

    @MockitoBean
    private AuthenticationFailureHandler authenticationFailureHandler;

    @MockitoBean
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private RememberMeServices rememberMeServices;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("userDetailsService")
    private UserDetailsService singleTenantUserDetailsService;

    @BeforeEach
    void beforeEach() {
        User userOne = new User();

        userOne.setLogin(USER_ONE_LOGIN);
        userOne.setPassword(RandomStringUtils.randomAlphanumeric(60));
        userOne.setActivated(true);
        userOne.setEmail(USER_ONE_EMAIL);
        userOne.setFirstName("userOne");
        userOne.setLastName("doe");
        userOne.setLangKey("en");

        userService.save(userOne);

        User userTwo = new User();

        userTwo.setLogin(USER_TWO_LOGIN);
        userTwo.setPassword(RandomStringUtils.randomAlphanumeric(60));
        userTwo.setActivated(true);
        userTwo.setEmail(USER_TWO_EMAIL);
        userTwo.setFirstName("userTwo");
        userTwo.setLastName("doe");
        userTwo.setLangKey("en");

        userService.save(userTwo);

        User userThree = new User();

        userThree.setLogin(USER_THREE_LOGIN);
        userThree.setPassword(RandomStringUtils.randomAlphanumeric(60));
        userThree.setActivated(false);
        userThree.setEmail(USER_THREE_EMAIL);
        userThree.setFirstName("userThree");
        userThree.setLastName("doe");
        userThree.setLangKey("en");

        userService.save(userThree);
    }

    @Test
    void assertThatUserCanBeFoundByLogin() {
        UserDetails userDetails = singleTenantUserDetailsService.loadUserByUsername(USER_ONE_LOGIN);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }

    @Test
    void assertThatUserCanBeFoundByLoginIgnoreCase() {
        UserDetails userDetails = singleTenantUserDetailsService.loadUserByUsername(
            USER_ONE_LOGIN.toUpperCase(Locale.ENGLISH));

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }

    @Test
    void assertThatUserCanBeFoundByEmail() {
        UserDetails userDetails = singleTenantUserDetailsService.loadUserByUsername(USER_TWO_EMAIL);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_TWO_LOGIN);
    }

    @Test
    void assertThatUserCanBeFoundByEmailIgnoreCase() {
        UserDetails userDetails = singleTenantUserDetailsService.loadUserByUsername(
            USER_TWO_EMAIL.toUpperCase(Locale.ENGLISH));

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_TWO_LOGIN);
    }

    @Test
    void assertThatEmailIsPrioritizedOverLogin() {
        UserDetails userDetails = singleTenantUserDetailsService.loadUserByUsername(USER_ONE_EMAIL);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }

    @Test
    void assertThatUserNotActivatedExceptionIsThrownForNotActivatedUsers() {
        assertThatExceptionOfType(UserNotActivatedException.class).isThrownBy(
            () -> singleTenantUserDetailsService.loadUserByUsername(USER_THREE_LOGIN));
    }
}
