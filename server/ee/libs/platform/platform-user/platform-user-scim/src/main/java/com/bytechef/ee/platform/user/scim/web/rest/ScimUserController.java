/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser;
import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser.ScimFilter;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimListResponse;
import com.bytechef.ee.platform.user.scim.model.ScimMeta;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchOperation;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * SCIM 2.0 User resource endpoints (RFC 7644 Section 3).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping(path = "/api/scim/v2/Users", produces = "application/scim+json")
class ScimUserController {

    private final UserService userService;

    @SuppressFBWarnings("EI")
    ScimUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    ResponseEntity<ScimListResponse<ScimUser>> listUsers(
        @RequestParam(defaultValue = "1") int startIndex,
        @RequestParam(defaultValue = "100") int count,
        @RequestParam(required = false) String filter) {

        if (filter != null) {
            ScimFilter scimFilter = ScimFilterParser.parse(filter);

            if (scimFilter != null && "userName".equalsIgnoreCase(scimFilter.attributeName())) {
                Optional<User> user = userService.fetchUserByLogin(scimFilter.value());

                if (user.isPresent()) {
                    return ResponseEntity.ok(ScimListResponse.of(List.of(toScimUser(user.get())), startIndex));
                }

                return ResponseEntity.ok(ScimListResponse.of(List.of(), startIndex));
            }

            if (scimFilter != null && "externalId".equalsIgnoreCase(scimFilter.attributeName())) {
                Optional<User> user = userService.fetchUserByEmail(scimFilter.value());

                if (user.isPresent()) {
                    return ResponseEntity.ok(ScimListResponse.of(List.of(toScimUser(user.get())), startIndex));
                }

                return ResponseEntity.ok(ScimListResponse.of(List.of(), startIndex));
            }
        }

        int pageIndex = Math.max(0, (startIndex - 1) / count);

        Page<User> userPage = userService.getAllActiveUsers(PageRequest.of(pageIndex, count));

        List<ScimUser> scimUsers = userPage.getContent()
            .stream()
            .map(this::toScimUser)
            .toList();

        return ResponseEntity.ok(ScimListResponse.of(scimUsers, startIndex));
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
        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setLogin(scimUser.userName());
        adminUserDTO.setEmail(extractPrimaryEmail(scimUser));
        adminUserDTO.setActivated(scimUser.active());

        if (scimUser.name() != null) {
            adminUserDTO.setFirstName(scimUser.name()
                .givenName());
            adminUserDTO.setLastName(scimUser.name()
                .familyName());
        }

        User createdUser = userService.create(adminUserDTO);

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

        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setId(id);
        adminUserDTO.setLogin(scimUser.userName());
        adminUserDTO.setEmail(extractPrimaryEmail(scimUser));
        adminUserDTO.setActivated(scimUser.active());

        if (scimUser.name() != null) {
            adminUserDTO.setFirstName(scimUser.name()
                .givenName());
            adminUserDTO.setLastName(scimUser.name()
                .familyName());
        }

        Optional<User> updatedUser = userService.update(adminUserDTO);

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

        User user = existingUser.get();

        for (ScimPatchOperation operation : patchRequest.operations()) {
            if ("replace".equalsIgnoreCase(operation.operation())) {
                applyReplaceOperation(user, operation);
            }
        }

        userService.save(user);

        return ResponseEntity.ok(toScimUser(user));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteUser(@PathVariable long id) {
        Optional<User> user = userService.fetchUser(id);

        if (user.isEmpty()) {
            return ResponseEntity.notFound()
                .build();
        }

        userService.delete(user.get()
            .getLogin());

        return ResponseEntity.noContent()
            .build();
    }

    @SuppressWarnings("unchecked")
    private void applyReplaceOperation(User user, ScimPatchOperation operation) {
        if (operation.path() != null) {
            switch (operation.path()) {
                case "active" -> user.setActivated(Boolean.parseBoolean(String.valueOf(operation.value())));
                case "userName" -> user.setLogin(String.valueOf(operation.value()));
                case "name.givenName" -> user.setFirstName(String.valueOf(operation.value()));
                case "name.familyName" -> user.setLastName(String.valueOf(operation.value()));
                default -> {
                }
            }
        } else if (operation.value() instanceof Map) {
            Map<String, Object> valueMap = (Map<String, Object>) operation.value();

            if (valueMap.containsKey("active")) {
                user.setActivated(Boolean.parseBoolean(String.valueOf(valueMap.get("active"))));
            }
        }
    }

    private String extractPrimaryEmail(ScimUser scimUser) {
        if (scimUser.emails() != null) {
            for (ScimEmail email : scimUser.emails()) {
                if (email.primary()) {
                    return email.value();
                }
            }

            if (!scimUser.emails()
                .isEmpty()) {
                return scimUser.emails()
                    .getFirst()
                    .value();
            }
        }

        return scimUser.userName();
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
