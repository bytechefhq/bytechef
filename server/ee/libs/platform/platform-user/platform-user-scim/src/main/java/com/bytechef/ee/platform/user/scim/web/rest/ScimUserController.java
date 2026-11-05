/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

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
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
 * SCIM 2.0 User resource endpoints (RFC 7644 Section 3).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping(path = "/api/scim/v2/Users", produces = "application/scim+json")
@ConditionalOnEEVersion
class ScimUserController {

    private static final String EXTERNAL_ID = "externalId";
    private static final String USER_NAME = "userName";

    private final ScimUserFacade scimUserFacade;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    ScimUserController(ScimUserFacade scimUserFacade, UserService userService) {
        this.scimUserFacade = scimUserFacade;
        this.userService = userService;
    }

    @GetMapping
    ResponseEntity<Object> listUsers(
        @RequestParam(defaultValue = "1") int startIndex,
        @RequestParam(defaultValue = "100") int count,
        @RequestParam(required = false) String filter) {

        int normalizedStartIndex = ScimPagination.normalizeStartIndex(startIndex);
        int normalizedCount = ScimPagination.normalizeCount(count);

        if (filter != null && !filter.isBlank()) {
            ScimFilter scimFilter = ScimFilterParser.parse(filter);

            if (scimFilter == null) {
                return ScimErrors.invalidFilter(filter);
            }

            Optional<User> user;

            if (USER_NAME.equalsIgnoreCase(scimFilter.attributeName())) {
                String value = scimFilter.value();

                user = userService.fetchUserByLogin(value.toLowerCase(Locale.ROOT));
            } else if (EXTERNAL_ID.equalsIgnoreCase(scimFilter.attributeName())) {
                user = userService.fetchUserByEmail(scimFilter.value());
            } else {
                return ScimErrors.invalidFilter(filter);
            }

            List<ScimUser> scimUsers = user.map(this::toScimUser)
                .stream()
                .toList();

            return ResponseEntity.ok(ScimListResponse.slice(scimUsers, normalizedStartIndex, normalizedCount));
        }

        if (normalizedCount == 0) {
            return ResponseEntity.ok(
                ScimListResponse.of(List.of(), userService.countActiveUsers(), normalizedStartIndex));
        }

        Page<User> userPage = userService.getAllActiveUsers(
            ScimOffsetPageRequest.of(normalizedStartIndex, normalizedCount));

        List<ScimUser> scimUsers = userPage.getContent()
            .stream()
            .map(this::toScimUser)
            .toList();

        return ResponseEntity.ok(
            ScimListResponse.of(scimUsers, userPage.getTotalElements(), normalizedStartIndex));
    }

    @GetMapping("/{id}")
    ResponseEntity<Object> getUser(@PathVariable long id) {
        Optional<User> user = userService.fetchUser(id);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(notFoundError("User", String.valueOf(id)));
        }

        return ResponseEntity.ok(toScimUser(user.get()));
    }

    @PostMapping
    ResponseEntity<ScimUser> createUser(@RequestBody ScimUser scimUser) {
        User createdUser = scimUserFacade.createUser(scimUser);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toScimUser(createdUser));
    }

    @PutMapping("/{id}")
    ResponseEntity<Object> replaceUser(@PathVariable long id, @RequestBody ScimUser scimUser) {
        Optional<User> existingUser = userService.fetchUser(id);

        if (existingUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(notFoundError("User", String.valueOf(id)));
        }

        Optional<User> updatedUser = ScimUserFacade.isDeactivation(existingUser.get(), scimUser)
            ? scimUserFacade.deactivateUser(id, scimUser) : scimUserFacade.replaceUser(id, scimUser);

        if (updatedUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(notFoundError("User", String.valueOf(id)));
        }

        return ResponseEntity.ok(toScimUser(updatedUser.get()));
    }

    @PatchMapping("/{id}")
    ResponseEntity<Object> patchUser(@PathVariable long id, @RequestBody ScimPatchRequest patchRequest) {
        Optional<User> existingUser = userService.fetchUser(id);

        if (existingUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(notFoundError("User", String.valueOf(id)));
        }

        Optional<User> patchedUser = ScimUserFacade.isDeactivation(existingUser.get(), patchRequest)
            ? scimUserFacade.deactivateUser(id, patchRequest) : scimUserFacade.patchUser(id, patchRequest);

        if (patchedUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(notFoundError("User", String.valueOf(id)));
        }

        return ResponseEntity.ok(toScimUser(patchedUser.get()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteUser(@PathVariable long id) {
        if (!scimUserFacade.deleteUser(id)) {
            return ResponseEntity.notFound()
                .build();
        }

        return ResponseEntity.noContent()
            .build();
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<Object> handleDuplicateKeyException() {
        return ScimErrors.uniqueness("A user with the same userName or email already exists");
    }

    private Map<String, Object> notFoundError(String resourceType, String id) {
        return Map.of(
            "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:Error"),
            "detail", resourceType + " " + id + " not found",
            "status", "404");
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
            List.of(ScimUser.SCHEMA), String.valueOf(user.getId()), user.getEmail(), user.getLogin(), scimName,
            scimName.formatted(), emails, user.isActivated(), List.of(), meta);
    }
}
