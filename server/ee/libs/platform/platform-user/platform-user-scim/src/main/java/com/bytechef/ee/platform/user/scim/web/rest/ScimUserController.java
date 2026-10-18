/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import com.bytechef.ee.platform.user.scim.ScimConstants;
import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser;
import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser.ScimFilter;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimListResponse;
import com.bytechef.ee.platform.user.scim.model.ScimMeta;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchOperation;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    private static final Set<String> UNSTORED_ATTRIBUTES = Set.of("displayName", "externalId", "name.formatted");

    private final UserService userService;

    @SuppressFBWarnings("EI")
    ScimUserController(UserService userService) {
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
        String userName = requireUserName(scimUser.userName());
        String email = extractPrimaryEmail(scimUser);

        if (userService.fetchUserByLogin(userName)
            .isPresent()) {

            throw ScimException.uniqueness("User " + userName + " already exists");
        }

        if (email != null && userService.fetchUserByEmail(email)
            .isPresent()) {

            throw ScimException.uniqueness("A user with email " + email + " already exists");
        }

        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setAuthorities(Set.of(AuthorityConstants.USER));
        adminUserDTO.setEmail(email);
        adminUserDTO.setLogin(userName);

        if (scimUser.name() != null) {
            adminUserDTO.setFirstName(scimUser.name()
                .givenName());
            adminUserDTO.setLastName(scimUser.name()
                .familyName());
        }

        User createdUser = userService.create(adminUserDTO);

        if (Boolean.FALSE.equals(scimUser.active())) {
            createdUser.setActivated(false);

            userService.save(createdUser);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toScimUser(createdUser));
    }

    @PutMapping("/{id}")
    ResponseEntity<ScimUser> replaceUser(@PathVariable long id, @RequestBody ScimUser scimUser) {
        User user = getExistingUser(id);

        setUserName(user, requireUserName(scimUser.userName()));
        setEmail(user, extractPrimaryEmail(scimUser));

        user.setFirstName(scimUser.name() == null ? null : scimUser.name()
            .givenName());
        user.setLastName(scimUser.name() == null ? null : scimUser.name()
            .familyName());

        if (scimUser.active() != null) {
            user.setActivated(scimUser.active());
        }

        userService.save(user);

        return ResponseEntity.ok(toScimUser(user));
    }

    @PatchMapping("/{id}")
    ResponseEntity<ScimUser> patchUser(@PathVariable long id, @RequestBody ScimPatchRequest patchRequest) {
        if (patchRequest == null || patchRequest.operations() == null || patchRequest.operations()
            .isEmpty()) {

            throw ScimException.invalidSyntax("PATCH request has no Operations");
        }

        User user = getExistingUser(id);

        for (ScimPatchOperation operation : patchRequest.operations()) {
            applyOperation(user, operation);
        }

        userService.save(user);

        return ResponseEntity.ok(toScimUser(user));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteUser(@PathVariable long id) {
        User user = getExistingUser(id);

        userService.delete(user.getLogin());

        return ResponseEntity.noContent()
            .build();
    }

    private void applyOperation(User user, ScimPatchOperation operation) {
        String op = operation.operation() == null ? "" : operation.operation()
            .toLowerCase(Locale.ROOT);

        switch (op) {
            case "add", "replace" -> {
                if (operation.path() == null) {
                    applyValueMap(user, operation.value());
                } else {
                    applyPath(user, operation.path(), operation.value());
                }
            }
            case "remove" -> removePath(user, operation.path());
            default -> throw ScimException.invalidSyntax("Unsupported PATCH op '" + operation.operation() + "'");
        }
    }

    @SuppressWarnings("unchecked")
    private void applyValueMap(User user, Object value) {
        if (!(value instanceof Map<?, ?> valueMap)) {
            throw ScimException.invalidSyntax("A PATCH operation without a path needs an object value");
        }

        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            String path = String.valueOf(entry.getKey());

            if ("name".equals(path) && entry.getValue() instanceof Map<?, ?> nameMap) {
                for (Map.Entry<?, ?> nameEntry : ((Map<Object, Object>) nameMap).entrySet()) {
                    applyPath(user, "name." + nameEntry.getKey(), nameEntry.getValue());
                }
            } else {
                applyPath(user, path, entry.getValue());
            }
        }
    }

    private void applyPath(User user, String path, Object value) {
        if (UNSTORED_ATTRIBUTES.contains(path)) {
            return;
        }

        switch (path) {
            case "active" -> user.setActivated(toBoolean(value));
            case "userName" -> setUserName(user, requireUserName(toText(value)));
            case "name.givenName" -> user.setFirstName(toText(value));
            case "name.familyName" -> user.setLastName(toText(value));
            case "emails", "emails[type eq \"work\"].value", "emails.value" -> setEmail(user, toEmail(value));
            default -> throw ScimException.invalidPath("Unsupported attribute '" + path + "'");
        }
    }

    private void removePath(User user, String path) {
        if (path == null) {
            throw ScimException.invalidSyntax("A remove operation needs a path");
        }

        if (UNSTORED_ATTRIBUTES.contains(path)) {
            return;
        }

        switch (path) {
            case "name.givenName" -> user.setFirstName(null);
            case "name.familyName" -> user.setLastName(null);
            case "active", "userName", "emails" -> throw ScimException.mutability(
                "Attribute '" + path + "' is required and cannot be removed");
            default -> throw ScimException.invalidPath("Unsupported attribute '" + path + "'");
        }
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
            .orElseThrow(() -> ScimException.notFound("User " + id + " not found"));
    }

    private void setEmail(User user, String email) {
        if (email == null || email.equalsIgnoreCase(user.getEmail())) {
            return;
        }

        userService.fetchUserByEmail(email)
            .filter(existingUser -> !Objects.equals(existingUser.getId(), user.getId()))
            .ifPresent(existingUser -> {
                throw ScimException.uniqueness("A user with email " + email + " already exists");
            });

        user.setEmail(email.toLowerCase(Locale.ROOT));
    }

    private void setUserName(User user, String userName) {
        String login = userName.toLowerCase(Locale.ROOT);

        if (login.equals(user.getLogin())) {
            return;
        }

        userService.fetchUserByLogin(login)
            .filter(existingUser -> !Objects.equals(existingUser.getId(), user.getId()))
            .ifPresent(existingUser -> {
                throw ScimException.uniqueness("User " + login + " already exists");
            });

        user.setLogin(login);
    }

    private static String extractPrimaryEmail(ScimUser scimUser) {
        if (scimUser.emails() != null && !scimUser.emails()
            .isEmpty()) {

            return scimUser.emails()
                .stream()
                .filter(ScimEmail::primary)
                .findFirst()
                .orElse(scimUser.emails()
                    .getFirst())
                .value();
        }

        String userName = scimUser.userName();

        return userName != null && userName.contains("@") ? userName : null;
    }

    private static Optional<Long> parseId(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException numberFormatException) {
            return Optional.empty();
        }
    }

    private static String requireUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            throw ScimException.invalidValue("userName is required");
        }

        return userName;
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (value instanceof String text && ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text))) {
            return Boolean.parseBoolean(text);
        }

        throw ScimException.invalidValue("'active' must be a boolean");
    }

    private static String toEmail(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                .findFirst()
                .or(() -> list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .findFirst())
                .map(email -> toText(email.get("value")))
                .orElse(null);
        }

        return toText(value);
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
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
