/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.audit;

import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ScimUserAuditMapper implements AuditMapper {

    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ScimUserAuditMapper(UserService userService) {
        this.userService = userService;
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        if (auditInvocation.event()
            .equals(ScimUserAuditEvents.SCIM_USER_CREATED)) {

            return null;
        }

        long id = (Long) auditInvocation.requireArgument("id");

        return userService.fetchUser(id)
            .map(UserState::of)
            .orElse(null);
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        UserState previousUserState = auditInvocation.captured() instanceof UserState userState ? userState : null;
        UserState currentUserState = toUserState(auditInvocation.result());

        if (auditInvocation.argument("id") instanceof Long id) {
            data.put("userId", String.valueOf(id));
        } else if (currentUserState != null && currentUserState.id() != null) {
            data.put("userId", String.valueOf(currentUserState.id()));
        }

        UserState userState = currentUserState == null ? previousUserState : currentUserState;

        if (userState != null) {
            putIfNotNull(data, "login", userState.login());
            putIfNotNull(data, "email", userState.email());

            if (!auditInvocation.event()
                .equals(ScimUserAuditEvents.SCIM_USER_DELETED)) {

                data.put("active", String.valueOf(userState.activated()));
            }
        } else if (auditInvocation.argument("scimUser") instanceof ScimUser scimUser) {
            putIfNotNull(data, "login", scimUser.userName());
        }

        if (auditInvocation.argument("scimUser") instanceof ScimUser scimUser) {
            putIfNotNull(data, "externalId", scimUser.externalId());
        }

        if (previousUserState != null && currentUserState != null) {
            putChanges(data, previousUserState, currentUserState);
        }

        return data;
    }

    @Override
    public boolean shouldRecord(AuditInvocation auditInvocation) {
        if (auditInvocation.outcome() != AuditOutcome.SUCCESS) {
            return true;
        }

        Object result = auditInvocation.result();

        return !(result instanceof Optional<?> optional && optional.isEmpty()) && !Boolean.FALSE.equals(result);
    }

    private static void putChanges(Map<String, String> data, UserState previousUserState, UserState userState) {
        List<String> changedFields = new ArrayList<>();

        if (!Objects.equals(previousUserState.login(), userState.login())) {
            changedFields.add("login");

            putIfNotNull(data, "previousLogin", previousUserState.login());
        }

        if (!Objects.equals(previousUserState.email(), userState.email())) {
            changedFields.add("email");

            putIfNotNull(data, "previousEmail", previousUserState.email());
        }

        if (!Objects.equals(previousUserState.firstName(), userState.firstName())) {
            changedFields.add("firstName");
        }

        if (!Objects.equals(previousUserState.lastName(), userState.lastName())) {
            changedFields.add("lastName");
        }

        if (previousUserState.activated() != userState.activated()) {
            changedFields.add("active");

            data.put("previousActive", String.valueOf(previousUserState.activated()));
        }

        if (!Objects.equals(previousUserState.authorityIds(), userState.authorityIds())) {
            changedFields.add("authorities");
        }

        data.put("changedFields", String.join(",", changedFields));
    }

    private static void putIfNotNull(Map<String, String> data, String key, @Nullable String value) {
        if (value != null) {
            data.put(key, value);
        }
    }

    private static @Nullable UserState toUserState(@Nullable Object result) {
        Object value = result instanceof Optional<?> optional ? optional.orElse(null) : result;

        return value instanceof User user ? UserState.of(user) : null;
    }

    private record UserState(
        @Nullable Long id, @Nullable String login, @Nullable String email, @Nullable String firstName,
        @Nullable String lastName, boolean activated, List<Long> authorityIds) {

        static UserState of(User user) {
            List<Long> authorityIds = user.getAuthorityIds();

            List<Long> sortedAuthorityIds = authorityIds == null ? List.of()
                : authorityIds.stream()
                    .sorted()
                    .toList();

            return new UserState(
                user.getId(), user.getLogin(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.isActivated(), sortedAuthorityIds);
        }
    }
}
