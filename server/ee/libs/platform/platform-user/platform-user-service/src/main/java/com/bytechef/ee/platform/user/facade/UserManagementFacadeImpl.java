/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.validator.PasswordValidator;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class UserManagementFacadeImpl implements UserManagementFacade {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AuthorityService authorityService;
    private final MailService mailService;
    private final TenantService tenantService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    UserManagementFacadeImpl(
        AuthorityService authorityService, MailService mailService, TenantService tenantService,
        UserService userService) {

        this.authorityService = authorityService;
        this.mailService = mailService;
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteUser(String login) {
        userService.delete(login);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void inviteUser(String email, String password, String role) {
        if (tenantService.isMultiTenantEnabled() && tenantService.tenantIdsByUserEmailExist(email)) {
            throw new EmailAlreadyUsedException();
        } else {
            Optional<User> user = userService.fetchUserByEmail(email);

            if (user.isPresent()) {
                throw new EmailAlreadyUsedException();
            }
        }

        PasswordValidator.validate(password);

        // Derive login from email if not provided
        String login = StringUtils.substringBefore(email, "@");

        AdminUserDTO userDTO = new AdminUserDTO();

        userDTO.setLogin(login);
        userDTO.setEmail(email);
        userDTO.setLangKey(UserConstants.DEFAULT_LANGUAGE);

        User user = userService.registerUser(userDTO, password);

        authorityService.getAuthorities()
            .stream()
            .filter(authority -> Objects.equals(authority.getName(), role))
            .findFirst()
            .ifPresent(authority -> {
                user.setAuthorities(Set.of(authority));
                userService.save(user);
            });

        Authority authority = authorityService.getAuthorities()
            .stream()
            .filter(curAuthority -> Objects.equals(curAuthority.getName(), role))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role));

        user.setAuthorities(Set.of(authority));

        userService.save(user);

        mailService.sendInvitationEmail(user, password);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Optional<UserWithAuthorities> fetchUser(String login) {
        List<Authority> authorities = authorityService.getAuthorities();

        return userService.fetchUserByLogin(login)
            .map(user -> new UserWithAuthorities(user, authorities));
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public UsersWithAuthorities getUsers(Integer pageNumber, Integer pageSize) {
        List<Authority> authorities = authorityService.getAuthorities();

        int page = pageNumber != null ? pageNumber : 0;
        int size = pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;

        Page<User> usersPage = userService.getAllManagedUsers(PageRequest.of(page, size));

        return new UsersWithAuthorities(usersPage, authorities);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public UserWithAuthorities updateUserRole(String login, String role) {
        List<Authority> allAuthorities = authorityService.getAuthorities();
        Optional<User> userOptional = userService.fetchUserByLogin(login);

        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        User user = userOptional.get();

        Authority authority = allAuthorities.stream()
            .filter(curAuthority -> Objects.equals(curAuthority.getName(), role))
            .findFirst()
            .orElseThrow();

        user.setAuthorities(Set.of(authority));

        User updatedUser = userService.update(new AdminUserDTO(user, allAuthorities))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return new UserWithAuthorities(updatedUser, allAuthorities);
    }
}
