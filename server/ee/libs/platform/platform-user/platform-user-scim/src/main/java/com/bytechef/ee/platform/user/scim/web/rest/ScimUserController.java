/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import com.bytechef.ee.platform.user.scim.ScimConstants;
import com.bytechef.ee.platform.user.scim.ScimException;
import com.bytechef.ee.platform.user.scim.facade.ScimUserFacade;
import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser;
import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser.ScimFilter;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimListResponse;
import com.bytechef.ee.platform.user.scim.model.ScimMeta;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SCIM 2.0 Users endpoint (RFC 7644). Every operation requires the {@code ROLE_SCIM} authority, which only the SCIM
 * bearer token filter grants, and runs in the tenant of that token.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping(path = "/api/scim/v2/Users", produces = "application/scim+json")
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
@PreAuthorize("hasAuthority(\"" + ScimConstants.SCIM_AUTHORITY + "\")")
class ScimUserController extends AbstractScimController {

    private final ScimUserFacade scimUserFacade;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    ScimUserController(ScimUserFacade scimUserFacade, UserService userService) {
        this.scimUserFacade = scimUserFacade;
        this.userService = userService;
    }

    @GetMapping
    ResponseEntity<ScimListResponse<ScimUser>> listUsers(
        @RequestParam(defaultValue = "1") int startIndex, @RequestParam(defaultValue = "100") int count,
        @RequestParam(required = false) String filter) {

        int normalizedStartIndex = Math.max(1, startIndex);

        if (filter != null && !filter.isBlank()) {
            ScimFilter scimFilter = ScimFilterParser.parse(filter);

            if (scimFilter == null) {
                throw ScimException.invalidFilter("Only 'attribute eq \"value\"' filters are supported");
            }

            List<ScimUser> scimUsers = findByFilter(scimFilter).map(this::toScimUser)
                .stream()
                .toList();

            return ResponseEntity.ok(ScimListResponse.of(scimUsers, normalizedStartIndex));
        }

        if (count <= 0) {
            Page<User> userPage = userService.getAllManagedUsers(PageRequest.of(0, 1));

            return ResponseEntity.ok(ScimListResponse.of(List.of(), normalizedStartIndex, userPage.getTotalElements()));
        }

        Page<User> userPage = userService.getAllManagedUsers(PageRequest.of((normalizedStartIndex - 1) / count, count));

        List<ScimUser> scimUsers = userPage.getContent()
            .stream()
            .map(this::toScimUser)
            .toList();

        return ResponseEntity.ok(ScimListResponse.of(scimUsers, normalizedStartIndex, userPage.getTotalElements()));
    }

    @GetMapping("/{id}")
    ResponseEntity<ScimUser> getUser(@PathVariable long id) {
        return ResponseEntity.ok(toScimUser(getExistingUser(id)));
    }

    @PostMapping
    ResponseEntity<ScimUser> createUser(@RequestBody ScimUser scimUser) {
        User createdUser = scimUserFacade.createUser(scimUser);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toScimUser(createdUser));
    }

    @PutMapping("/{id}")
    ResponseEntity<ScimUser> replaceUser(@PathVariable long id, @RequestBody ScimUser scimUser) {
        User user = getExistingUser(id);

        Optional<User> updatedUser = ScimUserFacade.isDeactivation(user, scimUser)
            ? scimUserFacade.deactivateUser(id, scimUser) : scimUserFacade.replaceUser(id, scimUser);

        return ResponseEntity.ok(toScimUser(updatedUser.orElseThrow(() -> userNotFound(id))));
    }

    @PatchMapping("/{id}")
    ResponseEntity<ScimUser> patchUser(@PathVariable long id, @RequestBody ScimPatchRequest patchRequest) {
        if (patchRequest == null || patchRequest.operations() == null || patchRequest.operations()
            .isEmpty()) {

            throw ScimException.invalidSyntax("PATCH request has no Operations");
        }

        User user = getExistingUser(id);

        Optional<User> patchedUser = ScimUserFacade.isDeactivation(user, patchRequest)
            ? scimUserFacade.deactivateUser(id, patchRequest) : scimUserFacade.patchUser(id, patchRequest);

        return ResponseEntity.ok(toScimUser(patchedUser.orElseThrow(() -> userNotFound(id))));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteUser(@PathVariable long id) {
        if (!scimUserFacade.deleteUser(id)) {
            throw userNotFound(id);
        }

        return ResponseEntity.noContent()
            .build();
    }

    private Optional<User> findByFilter(ScimFilter scimFilter) {
        String value = scimFilter.value();

        return switch (scimFilter.attributeName()) {
            case "userName" -> userService.fetchUserByLogin(value.toLowerCase(Locale.ROOT));
            case "emails.value", "emails", "externalId" -> userService.fetchUserByEmail(value);
            case "id" -> parseId(value).flatMap(userService::fetchUser);
            default -> throw ScimException.invalidFilter(
                "Filtering by '" + scimFilter.attributeName() + "' is not supported");
        };
    }

    private User getExistingUser(long id) {
        return userService.fetchUser(id)
            .orElseThrow(() -> userNotFound(id));
    }

    private static ScimException userNotFound(long id) {
        return ScimException.notFound("User " + id + " not found");
    }

    private static Optional<Long> parseId(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException numberFormatException) {
            return Optional.empty();
        }
    }

    private ScimUser toScimUser(User user) {
        ScimName scimName = new ScimName(
            ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "")).trim(),
            user.getLastName(), user.getFirstName());

        List<ScimEmail> emails = List.of();

        if (user.getEmail() != null) {
            emails = List.of(new ScimEmail(user.getEmail(), "work", true));
        }

        ScimMeta meta = new ScimMeta(
            "User",
            user.getCreatedDate() != null ? user.getCreatedDate()
                .toString() : null,
            user.getLastModifiedDate() != null ? user.getLastModifiedDate()
                .toString() : null,
            "/api/scim/v2/Users/" + user.getId());

        return new ScimUser(
            List.of(ScimUser.SCHEMA), String.valueOf(user.getId()), null, user.getLogin(), scimName,
            scimName.formatted(), emails, user.isActivated(), List.of(), meta);
    }
}
