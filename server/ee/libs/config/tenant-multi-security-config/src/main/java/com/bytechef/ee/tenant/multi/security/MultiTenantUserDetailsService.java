/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.ee.tenant.multi.security;

import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.UserNotActivatedException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.tenant.util.TenantUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Authenticate a user from the database.
 *
 * @author Ivica Cardic
 */
public class MultiTenantUserDetailsService implements UserDetailsService, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantUserDetailsService.class);

    private ApplicationContext applicationContext;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public MultiTenantUserDetailsService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public UserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);

        EmailValidator emailValidator = EmailValidator.getInstance();

        UserService userService = applicationContext.getBean(UserService.class);

        if (emailValidator.isValid(login)) {
            List<String> tenantIds = tenantService.getTenantIdsByUserEmail(login);

            if (tenantIds.isEmpty()) {
                throw new UsernameNotFoundException("User with email " + login + " was not found in the database");
            }

            return TenantUtils.callWithTenantId(
                tenantIds.getFirst(),
                () -> userService.fetchUserByEmail(login)
                    .map(user -> createSpringSecurityUser(login, user))
                    .orElseThrow(() -> new UsernameNotFoundException(
                        "User with email " + login + " was not found in the database")));
        }

        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);

        List<String> tenantIds = tenantService.getTenantIdsByUserLogin(lowercaseLogin);

        if (tenantIds.isEmpty()) {
            throw new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database");
        }

        return TenantUtils.callWithTenantId(
            tenantIds.getFirst(),
            () -> userService.fetchUserByLogin(lowercaseLogin)
                .map(user -> createSpringSecurityUser(lowercaseLogin, user))
                .orElseThrow(
                    () -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database")));
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(
        String lowercaseLogin, User user) {

        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
        }

        AuthorityService authorityService = applicationContext.getBean(AuthorityService.class);

        List<SimpleGrantedAuthority> grantedAuthorities = user.getAuthorityIds()
            .stream()
            .map(authorityService::fetchAuthority)
            .map(Optional::get)
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .toList();

        return new org.springframework.security.core.userdetails.User(
            user.getLogin(), user.getPassword(), grantedAuthorities);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
