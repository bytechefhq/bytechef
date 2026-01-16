/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.web.graphql;

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
import com.bytechef.platform.user.util.PasswordValidator;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

/**
 * GraphQL controller for managing Users.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class UserGraphQlController {

    private final AuthorityService authorityService;
    private final UserService userService;
    private final MailService mailService;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public UserGraphQlController(
        AuthorityService authorityService, UserService userService, MailService mailService,
        TenantService tenantService) {

        this.authorityService = authorityService;
        this.userService = userService;
        this.mailService = mailService;
        this.tenantService = tenantService;
    }

    @MutationMapping(name = "deleteUser")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Boolean deleteUser(@Argument String login) {
        userService.delete(login);

        return true;
    }

    @MutationMapping(name = "inviteUser")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Boolean inviteUser(@Argument String email, @Argument String password, @Argument String role) {
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

        return true;
    }

    @MutationMapping(name = "updateUser")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AdminUserDTO updateUser(@Argument String login, @Argument String role) {
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

        return userService.update(new AdminUserDTO(user, allAuthorities))
            .map(curUser -> new AdminUserDTO(curUser, allAuthorities))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @QueryMapping(name = "user")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AdminUserDTO user(@Argument String login) {
        List<Authority> authorities = authorityService.getAuthorities();

        Optional<User> userOptional = userService.fetchUserByLogin(login);

        return userOptional.map(user -> new AdminUserDTO(user, authorities))
            .orElse(null);
    }

    @QueryMapping(name = "users")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AdminUserPage users(@Argument Integer pageNumber, @Argument Integer pageSize) {
        List<Authority> authorities = authorityService.getAuthorities();

        int page = pageNumber != null ? pageNumber : 0;
        int size = pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;

        Page<User> usersPage = userService.getAllManagedUsers(PageRequest.of(page, size));

        List<AdminUserDTO> content = usersPage.getContent()
            .stream()
            .map(user -> new AdminUserDTO(user, authorities))
            .collect(Collectors.toList());

        return new AdminUserPage(
            content, usersPage.getTotalElements(), usersPage.getTotalPages(), usersPage.getNumber(),
            usersPage.getSize());
    }

    private static final int DEFAULT_PAGE_SIZE = 20;

    @SuppressFBWarnings("EI")
    public record AdminUserPage(
        List<AdminUserDTO> content, long totalElements, int totalPages, int number, int size) {
    }
}
