/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.facade;

import com.bytechef.ee.platform.user.scim.ScimException;
import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditEvents;
import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditMapper;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchOperation;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
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
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@Transactional
public class ScimUserFacade {

    private static final String ACTIVE = "active";
    private static final Set<String> UNSTORED_ATTRIBUTES = Set.of("displayName", "externalId", "name.formatted");

    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ScimUserFacade(UserService userService) {
        this.userService = userService;
    }

    public static boolean isDeactivation(User user, ScimUser scimUser) {
        return user.isActivated() && Boolean.FALSE.equals(scimUser.active());
    }

    public static boolean isDeactivation(User user, ScimPatchRequest scimPatchRequest) {
        if (!user.isActivated()) {
            return false;
        }

        Boolean active = null;

        for (ScimPatchOperation scimPatchOperation : scimPatchRequest.operations()) {
            String operation = toOperation(scimPatchOperation);

            if (!operation.equals("add") && !operation.equals("replace")) {
                continue;
            }

            String path = scimPatchOperation.path();
            Object value = scimPatchOperation.value();

            if (ACTIVE.equals(path)) {
                active = parseBoolean(value);
            } else if (path == null && value instanceof Map<?, ?> valueMap && valueMap.containsKey(ACTIVE)) {
                active = parseBoolean(valueMap.get(ACTIVE));
            }
        }

        return Boolean.FALSE.equals(active);
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_CREATED, mapper = ScimUserAuditMapper.class)
    public User createUser(ScimUser scimUser) {
        String userName = requireUserName(scimUser.userName());
        String email = extractPrimaryEmail(scimUser);

        Optional<User> userWithLogin = userService.fetchUserByLogin(userName);

        if (userWithLogin.isPresent()) {
            throw ScimException.uniqueness("User " + userName + " already exists");
        }

        if (email != null) {
            Optional<User> userWithEmail = userService.fetchUserByEmail(email);

            if (userWithEmail.isPresent()) {
                throw ScimException.uniqueness("A user with email " + email + " already exists");
            }
        }

        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setAuthorities(Set.of(AuthorityConstants.USER));
        adminUserDTO.setEmail(email);
        adminUserDTO.setLogin(userName);

        ScimName scimName = scimUser.name();

        if (scimName != null) {
            adminUserDTO.setFirstName(scimName.givenName());
            adminUserDTO.setLastName(scimName.familyName());
        }

        User createdUser = userService.create(adminUserDTO);

        if (Boolean.FALSE.equals(scimUser.active())) {
            createdUser.setActivated(false);

            userService.save(createdUser);
        }

        return createdUser;
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_DEACTIVATED, mapper = ScimUserAuditMapper.class)
    public Optional<User> deactivateUser(long id, ScimPatchRequest scimPatchRequest) {
        return applyPatch(id, scimPatchRequest);
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_DEACTIVATED, mapper = ScimUserAuditMapper.class)
    public Optional<User> deactivateUser(long id, ScimUser scimUser) {
        return applyReplace(id, scimUser);
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_DELETED, mapper = ScimUserAuditMapper.class)
    public boolean deleteUser(long id) {
        Optional<User> userOptional = userService.fetchUser(id);

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        userService.delete(user.getLogin());

        return true;
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_PATCHED, mapper = ScimUserAuditMapper.class)
    public Optional<User> patchUser(long id, ScimPatchRequest scimPatchRequest) {
        return applyPatch(id, scimPatchRequest);
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_UPDATED, mapper = ScimUserAuditMapper.class)
    public Optional<User> replaceUser(long id, ScimUser scimUser) {
        return applyReplace(id, scimUser);
    }

    private Optional<User> applyPatch(long id, ScimPatchRequest scimPatchRequest) {
        Optional<User> userOptional = userService.fetchUser(id);

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();

        for (ScimPatchOperation scimPatchOperation : scimPatchRequest.operations()) {
            applyOperation(user, scimPatchOperation);
        }

        userService.save(user);

        return Optional.of(user);
    }

    private Optional<User> applyReplace(long id, ScimUser scimUser) {
        Optional<User> userOptional = userService.fetchUser(id);

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();

        setUserName(user, requireUserName(scimUser.userName()));
        setEmail(user, extractPrimaryEmail(scimUser));

        ScimName scimName = scimUser.name();

        user.setFirstName(scimName == null ? null : scimName.givenName());
        user.setLastName(scimName == null ? null : scimName.familyName());

        if (scimUser.active() != null) {
            user.setActivated(scimUser.active());
        }

        userService.save(user);

        return Optional.of(user);
    }

    private void applyOperation(User user, ScimPatchOperation scimPatchOperation) {
        String path = scimPatchOperation.path();

        switch (toOperation(scimPatchOperation)) {
            case "add", "replace" -> {
                if (path == null) {
                    applyValueMap(user, scimPatchOperation.value());
                } else {
                    applyPath(user, path, scimPatchOperation.value());
                }
            }
            case "remove" -> removePath(user, path);
            default -> throw ScimException.invalidSyntax(
                "Unsupported PATCH op '" + scimPatchOperation.operation() + "'");
        }
    }

    private void applyPath(User user, String path, @Nullable Object value) {
        if (UNSTORED_ATTRIBUTES.contains(path)) {
            return;
        }

        switch (path) {
            case ACTIVE -> user.setActivated(toBoolean(value));
            case "userName" -> setUserName(user, requireUserName(toText(value)));
            case "name.givenName" -> user.setFirstName(toText(value));
            case "name.familyName" -> user.setLastName(toText(value));
            case "emails", "emails[type eq \"work\"].value", "emails.value" -> setEmail(user, toEmail(value));
            default -> throw ScimException.invalidPath("Unsupported attribute '" + path + "'");
        }
    }

    private void applyValueMap(User user, @Nullable Object value) {
        if (!(value instanceof Map<?, ?> valueMap)) {
            throw ScimException.invalidSyntax("A PATCH operation without a path needs an object value");
        }

        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            String path = String.valueOf(entry.getKey());

            if ("name".equals(path) && entry.getValue() instanceof Map<?, ?> nameMap) {
                for (Map.Entry<?, ?> nameEntry : nameMap.entrySet()) {
                    applyPath(user, "name." + nameEntry.getKey(), nameEntry.getValue());
                }
            } else {
                applyPath(user, path, entry.getValue());
            }
        }
    }

    private void removePath(User user, @Nullable String path) {
        if (path == null) {
            throw ScimException.invalidSyntax("A remove operation needs a path");
        }

        if (UNSTORED_ATTRIBUTES.contains(path)) {
            return;
        }

        switch (path) {
            case "name.givenName" -> user.setFirstName(null);
            case "name.familyName" -> user.setLastName(null);
            case ACTIVE, "userName", "emails" -> throw ScimException.mutability(
                "Attribute '" + path + "' is required and cannot be removed");
            default -> throw ScimException.invalidPath("Unsupported attribute '" + path + "'");
        }
    }

    private void setEmail(User user, @Nullable String email) {
        if (email == null || email.equalsIgnoreCase(user.getEmail())) {
            return;
        }

        Optional<User> otherUserWithEmail = userService.fetchUserByEmail(email)
            .filter(existingUser -> !Objects.equals(existingUser.getId(), user.getId()));

        if (otherUserWithEmail.isPresent()) {
            throw ScimException.uniqueness("A user with email " + email + " already exists");
        }

        user.setEmail(email.toLowerCase(Locale.ROOT));
    }

    private void setUserName(User user, String userName) {
        String login = userName.toLowerCase(Locale.ROOT);

        if (login.equals(user.getLogin())) {
            return;
        }

        Optional<User> otherUserWithLogin = userService.fetchUserByLogin(login)
            .filter(existingUser -> !Objects.equals(existingUser.getId(), user.getId()));

        if (otherUserWithLogin.isPresent()) {
            throw ScimException.uniqueness("User " + login + " already exists");
        }

        user.setLogin(login);
    }

    private static @Nullable String extractPrimaryEmail(ScimUser scimUser) {
        List<ScimEmail> scimEmails = scimUser.emails();

        if (scimEmails != null && !scimEmails.isEmpty()) {
            for (ScimEmail scimEmail : scimEmails) {
                if (scimEmail.primary()) {
                    return scimEmail.value();
                }
            }

            ScimEmail scimEmail = scimEmails.getFirst();

            return scimEmail.value();
        }

        String userName = scimUser.userName();

        return userName != null && userName.contains("@") ? userName : null;
    }

    private static @Nullable Boolean parseBoolean(@Nullable Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (value instanceof String text && ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text))) {
            return Boolean.parseBoolean(text);
        }

        return null;
    }

    private static String requireUserName(@Nullable String userName) {
        if (userName == null || userName.isBlank()) {
            throw ScimException.invalidValue("userName is required");
        }

        return userName;
    }

    private static boolean toBoolean(@Nullable Object value) {
        Boolean booleanValue = parseBoolean(value);

        if (booleanValue == null) {
            throw ScimException.invalidValue("'active' must be a boolean");
        }

        return booleanValue;
    }

    private static @Nullable String toEmail(@Nullable Object value) {
        if (value instanceof List<?> list) {
            List<Map<?, ?>> emails = list.stream()
                .filter(Map.class::isInstance)
                .<Map<?, ?>>map(Map.class::cast)
                .toList();

            return emails.stream()
                .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                .findFirst()
                .or(() -> emails.stream()
                    .findFirst())
                .map(email -> toText(email.get("value")))
                .orElse(null);
        }

        return toText(value);
    }

    private static String toOperation(ScimPatchOperation scimPatchOperation) {
        String operation = scimPatchOperation.operation();

        return operation == null ? "" : operation.toLowerCase(Locale.ROOT);
    }

    private static @Nullable String toText(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
